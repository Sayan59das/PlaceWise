// Screen 7: the multithreaded placement drive, the race-condition demo and the thread-lifecycle demo.
// The threads run inside the Java server; this page shows what they produced.

let driveTab = 'drive';

async function showDrive(screen) {
  const tabs = [['drive', 'Placement drive'], ['race', 'Race condition'], ['lifecycle', 'Thread lifecycle']];
  const buttons = tabs.map(([id, label]) =>
    `<button class="btn" data-tab="${id}" aria-pressed="${driveTab === id}">${label}</button>`).join('');
  screen.innerHTML = titleRow('Placement drive',
    `<div class="segmented" role="group" aria-label="Demo">${buttons}</div>`) + '<div id="drive-body"></div>';
  for (const button of screen.querySelectorAll('[data-tab]')) {
    button.onclick = () => { driveTab = button.dataset.tab; run(() => showDrive(screen)); };
  }
  if (driveTab === 'drive') {
    showDriveForm();
  } else if (driveTab === 'race') {
    showRaceForm();
  } else {
    showLifecycle();
  }
}

// ---------- The placement drive ----------

function showDriveForm() {
  document.getElementById('drive-body').innerHTML = `
    <form id="drive-form" class="sheet form-grid">
      <fieldset class="wide">
        <legend>Which data?</legend>
        <label class="choice"><input type="radio" name="source" value="generated" checked>
          Generated students and companies (nothing is saved)</label>
        <label class="choice"><input type="radio" name="source" value="stored">
          The stored students and companies (the results are saved and appear in the reports)</label>
      </fieldset>
      <label class="field">Students<input name="students" inputmode="numeric" value="200"></label>
      <label class="field">Companies<input name="companies" inputmode="numeric" value="5"></label>
      <label class="field">Seats per company<input name="seats" inputmode="numeric" value="8"></label>
      <label class="field">Recruiter threads<input name="recruiters" inputmode="numeric" value="4"></label>
      <label class="field">Random seed<input name="seed" inputmode="numeric" value="42"></label>
      <div class="form-actions"><button class="btn primary" type="submit">Run the drive</button></div>
    </form>
    <div id="drive-result"></div>`;
  const form = document.getElementById('drive-form');
  const sizeInputs = form.querySelectorAll('[name="students"], [name="companies"], [name="seats"]');
  // with stored data the sizes come from the data itself, so those boxes are switched off
  for (const radio of form.querySelectorAll('[name="source"]')) {
    radio.onchange = () => {
      for (const input of sizeInputs) {
        input.disabled = form.source.value === 'stored';
      }
    };
  }
  form.onsubmit = event => {
    event.preventDefault();
    run(() => busy(form.querySelector('[type="submit"]'), async () => {
      const path = form.source.value === 'stored' ? '/api/drive/stored' : '/api/drive/generated';
      const result = await api('POST', path, new FormData(form));
      document.getElementById('drive-result').innerHTML = driveResult(result);
    }));
  };
}

function driveResult(d) {
  const facts = `
    <ul class="facts">
      <li><b>${d.applications}</b> applications</li>
      <li><b>${d.selected}</b> selected</li>
      <li><b>${d.waitlisted}</b> waitlisted</li>
      <li><b>${d.rejected}</b> rejected</li>
      <li><b>${d.withdrawn}</b> withdrawn</li>
      <li><b>${d.placedStudents}</b> students placed</li>
      <li><b>${d.recruiters}</b> recruiter threads, ${d.elapsedMs} ms</li>
    </ul>`;
  const verdict = d.violations.length === 0
    ? `<p class="verdict">All invariants hold: no company over its seats, nobody selected twice, every application
         has one final status, only eligible students selected, the waitlist never outranks the selected.</p>`
    : `<p class="verdict bad">Invariant violations:</p><ul>${d.violations.map(v => `<li>${esc(v)}</li>`).join('')}</ul>`;
  const progress = d.progress.length
    ? `<h2 class="section-title">What the progress-monitor thread printed</h2><pre class="log">${esc(d.progress.join('\n'))}</pre>`
    : '';
  return facts + verdict + `<p class="note">${esc(d.persistence)}</p>`
    + `<h2 class="section-title">Seats per company</h2><div class="sides">${d.boards.map(seatSide).join('')}</div>`
    + progress;
}

// One company as a side of a tape: exactly as many ruled lines as seats, the waitlist pencilled in below.
const WAITLIST_SHOWN = 3;

function seatSide(board) {
  const seatRows = [];
  for (let seat = 0; seat < board.seats; seat++) {
    const s = board.selected[seat];
    if (s) {
      seatRows.push({ title: esc(s.name), detail: `${esc(s.rollNo)} · ${esc(s.branch)}`, figure: twoDecimals(s.cgpa) });
    } else {
      seatRows.push({ title: 'Open seat', pencil: true });
    }
  }
  const waitRows = board.waitlisted.slice(0, WAITLIST_SHOWN).map(s => ({
    title: esc(s.name), detail: `${esc(s.rollNo)} · ${esc(s.branch)}`, figure: twoDecimals(s.cgpa), pencil: true,
  }));
  const hidden = board.waitlisted.length - waitRows.length;
  const waitlist = waitRows.length
    ? '<p class="tape-end">end of seats · waitlist</p>' + tapeSide(null, '', waitRows, '')
    : '';
  return `<div class="side">
      ${tapeSide(esc(board.company), `${board.selected.length} of ${board.seats} seats`, seatRows, '')}
      ${waitlist}
      ${hidden > 0 ? `<p class="more">and ${hidden} more on the waitlist</p>` : ''}
    </div>`;
}

// ---------- The race-condition demo ----------

function showRaceForm() {
  document.getElementById('drive-body').innerHTML = `
    <form id="race-form" class="sheet form-grid">
      <label class="field">Competing threads<input name="threads" inputmode="numeric" value="8"></label>
      <label class="field">Seats per company<input name="seats" inputmode="numeric" value="2"></label>
      <label class="field">Pause between check and update (ms)<input name="window" inputmode="numeric" value="5"></label>
      <p class="note">The same threads try to take the same seats twice: once with no lock, once with one lock per company.</p>
      <div class="form-actions"><button class="btn primary" type="submit">Run both</button></div>
    </form>
    <div id="race-result"></div>`;
  const form = document.getElementById('race-form');
  form.onsubmit = event => {
    event.preventDefault();
    run(() => busy(form.querySelector('[type="submit"]'), async () => {
      const r = await api('POST', '/api/drive/race', new FormData(form));
      const scale = Math.max(r.unsafeGranted, r.safeGranted, r.totalSeats);
      const raceShown = r.unsafeOversold > 0 && r.safeOversold === 0;
      document.getElementById('race-result').innerHTML = `
        <p class="note">${r.attempts} attempts by ${r.threads} threads for ${r.totalSeats} real seats.
          ${r.runs > 1 ? `The unsafe run was lucky ${r.runs - 1} time(s), so it ran again.` : ''}</p>
        <p class="note">The dark line marks the real seats. Red hatching is a seat that was sold twice.</p>
        <div class="columns">
          ${raceSide('Without a lock', r.unsafeGranted, r.unsafeOversold, r.totalSeats, scale)}
          ${raceSide('With a lock per company', r.safeGranted, r.safeOversold, r.totalSeats, scale)}
        </div>
        <p class="verdict${raceShown ? '' : ' bad'}">${raceShown
          ? 'Same workload, different result: that is a race condition, and the lock cures it.'
          : 'No oversell showed up this time. Raise the pause or the thread count and run again.'}</p>`;
    }));
  };
}

// Seats as a bar. The dark line is the real number of seats: anything past it was sold twice.
function raceSide(title, granted, oversold, seats, scale) {
  const fit = Math.min(granted, seats);
  return `<section class="side">
      <h2 class="side-head"><span>${title}</span><span>${granted} granted</span></h2>
      <div class="capacity" role="img" aria-label="${granted} seats granted, ${seats} real seats">
        <span class="fill" style="width: ${fit / scale * 100}%"></span>
        <span class="over" style="width: ${oversold / scale * 100}%"></span>
        <span class="limit" style="left: ${seats / scale * 100}%"></span>
        <span class="limit-label ${seats / scale > 0.5 ? 'before' : 'after'}" style="left: ${seats / scale * 100}%">real seats: ${seats}</span>
      </div>
      <p class="${oversold > 0 ? 'verdict bad' : 'verdict'}">${oversold > 0
        ? `Oversold ${plural(oversold, 'seat', 'seats')}`
        : 'Oversold 0 seats'}</p>
    </section>`;
}

// ---------- The thread-lifecycle demo ----------

function showLifecycle() {
  document.getElementById('drive-body').innerHTML = `
    <div class="sheet">
      <p class="note">One worker thread is walked through every state while the main thread watches it.</p>
      <div class="actions-row"><button class="btn primary" id="lifecycle-run">Run the demo</button></div>
    </div>
    <div id="lifecycle-result"></div>`;
  const button = document.getElementById('lifecycle-run');
  button.onclick = () => run(() => busy(button, async () => {
    const answer = await api('POST', '/api/drive/lifecycle');
    const rows = answer.states.map(s => ({ title: esc(s.state), detail: esc(s.meaning) }));
    document.getElementById('lifecycle-result').innerHTML =
      tapeSide('States seen, in order', plural(rows.length, 'state', 'states'), rows, '');
  }));
}
