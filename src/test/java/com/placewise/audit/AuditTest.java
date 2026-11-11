package com.placewise.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.placewise.exception.PlaceWiseException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class AuditTest {

    @TempDir
    Path tmp;

    @Test
    void aLineHasTimestampThreadEventAndMessage() throws IOException {
        Path file = tmp.resolve("audit.log");
        try (FileAuditLog log = new FileAuditLog(file)) {
            Thread t = new Thread(() -> log.log(AuditEvent.SELECTED, "R1 -> Acme"), "recruiter-9");
            t.start();
            t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0)).matches("\\d{4}-\\d{2}-\\d{2}T[\\d:.]+Z \\| recruiter-9 \\| SELECTED \\| R1 -> Acme");
    }

    @Test
    void lineBreaksInAMessageCannotForgeExtraLogLines() throws IOException {
        Path file = tmp.resolve("audit.log");
        try (FileAuditLog log = new FileAuditLog(file)) {
            log.log(AuditEvent.APPLICATION, "evil\n2026-01-01T00:00:00Z | admin | SELECTED | forged\r\nmore");
        }
        assertThat(Files.readAllLines(file)).hasSize(1);
    }

    @Test
    void theLogIsAppendOnlyAcrossRuns() throws IOException {
        Path file = tmp.resolve("sub/dir/audit.log"); // parent folders are created on demand
        try (FileAuditLog first = new FileAuditLog(file)) {
            first.log(AuditEvent.APPLICATION, "one");
        }
        try (FileAuditLog second = new FileAuditLog(file)) {
            second.log(AuditEvent.WITHDRAWN, "two");
            assertThat(second.file()).isEqualTo(file);
        }
        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0)).endsWith("| APPLICATION | one");
        assertThat(lines.get(1)).endsWith("| WITHDRAWN | two");
    }

    @Test
    void loggingAfterCloseIsAnErrorAndCloseIsIdempotent() {
        FileAuditLog log = new FileAuditLog(tmp.resolve("audit.log"));
        log.close();
        log.close();
        assertThatThrownBy(() -> log.log(AuditEvent.APPLICATION, "late")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void anUnopenableLogFileFailsClearly() throws IOException {
        Path blocker = tmp.resolve("afile");
        Files.writeString(blocker, "x");
        assertThatThrownBy(() -> new FileAuditLog(blocker.resolve("audit.log")))
                .isInstanceOf(PlaceWiseException.class).hasMessageContaining("Cannot open the audit log");
    }

    @Test
    void theNoOpLogAcceptsEverything() {
        AuditLog none = AuditLog.none();
        none.log(AuditEvent.SELECTED, "ignored");
        none.close();
    }

    @Test
    @Timeout(60)
    void eightThreadsWritingNeverInterleaveOrLoseLines() throws Exception {
        Path file = tmp.resolve("audit.log");
        int threads = 8;
        int perThread = 1_000;
        Pattern line = Pattern.compile("^\\S+ \\| writer-(\\d) \\| (APPLICATION|SELECTED) \\| payload-(\\d)-(\\d+)-x{40}$");
        try (FileAuditLog log = new FileAuditLog(file)) {
            List<Thread> workers = new ArrayList<>();
            Object startGate = new Object();
            boolean[] go = {false};
            for (int t = 0; t < threads; t++) {
                int id = t;
                Thread worker = new Thread(() -> {
                    synchronized (startGate) {
                        while (!go[0]) {
                            try {
                                startGate.wait();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                    }
                    for (int i = 0; i < perThread; i++) {
                        // a long message makes a torn write very likely if the lock were missing
                        log.log(i % 2 == 0 ? AuditEvent.APPLICATION : AuditEvent.SELECTED,
                                "payload-" + id + "-" + i + "-" + "x".repeat(40));
                    }
                }, "writer-" + id);
                workers.add(worker);
                worker.start();
            }
            synchronized (startGate) {
                go[0] = true;
                startGate.notifyAll();
            }
            for (Thread worker : workers) {
                worker.join();
            }
        }
        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(threads * perThread);
        Set<String> payloads = new HashSet<>();
        for (String text : lines) {
            var matcher = line.matcher(text);
            assertThat(matcher.matches()).as("intact line: %s", text).isTrue();
            // the thread name in the line must be the thread that produced the payload
            assertThat(matcher.group(1)).isEqualTo(matcher.group(3));
            payloads.add(matcher.group(3) + "-" + matcher.group(4));
        }
        assertThat(payloads).hasSize(threads * perThread); // nothing lost, nothing duplicated
    }

    @Test
    void backupIsAByteForByteCopyLargerThanTheBuffer() throws IOException {
        Path source = tmp.resolve("source.bin");
        byte[] data = new byte[20_001];
        new Random(1).nextBytes(data);
        Files.write(source, data);
        Path target = tmp.resolve("backups/copy.bin");
        assertThat(AuditBackup.copy(source, target)).isEqualTo(20_001);
        assertThat(Files.readAllBytes(target)).isEqualTo(data);
    }

    @Test
    void backupOfAnEmptyFileAndOverwriteOfAnOldBackup() throws IOException {
        Path source = tmp.resolve("empty.log");
        Files.createFile(source);
        Path target = tmp.resolve("old.bak");
        Files.writeString(target, "stale content that must disappear");
        assertThat(AuditBackup.copy(source, target)).isZero();
        assertThat(Files.size(target)).isZero();
    }

    @Test
    void backupOfAMissingFileFailsClearly() {
        assertThatThrownBy(() -> AuditBackup.copy(tmp.resolve("missing.log"), tmp.resolve("x.bak")))
                .isInstanceOf(PlaceWiseException.class).hasMessageContaining("Nothing to back up");
    }

    @Test
    void backupIntoAnUnwritableLocationFailsClearly() throws IOException {
        Path source = tmp.resolve("a.log");
        Files.writeString(source, "x");
        Path blocker = tmp.resolve("afile");
        Files.writeString(blocker, "x");
        assertThatThrownBy(() -> AuditBackup.copy(source, blocker.resolve("b.bak")))
                .isInstanceOf(PlaceWiseException.class).hasMessageContaining("Backup of");
    }

    @Test
    void aLiveLogCanBeBackedUpAndTheCopyHoldsEveryFinishedLine() throws IOException {
        Path file = tmp.resolve("audit.log");
        try (FileAuditLog log = new FileAuditLog(file)) {
            for (int i = 0; i < 50; i++) {
                log.log(AuditEvent.APPLICATION, "n" + i);
            }
            AuditBackup.copy(file, tmp.resolve("audit.bak"));
        }
        assertThat(Files.readAllLines(tmp.resolve("audit.bak"))).hasSize(50);
    }
}
