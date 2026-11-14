// Screen 3: one student against every company, or every student against one company.
// The result is a J-card opened flat: Side A = eligible, Side B = not eligible with the exact gap in red.

let eligibilityMode = 'student';

async function showEligibility(screen) {
  const [students, companies] = await Promise.all([loadStudents(), loadCompanies()]);
  if (students.length === 0 || companies.length === 0) {
    needData(screen, 'Check eligibility');
    return;
  }
  const picker = eligibilityMode === 'student' ? studentPicker(students) : companyPicker(companies);
  screen.innerHTML = titleRow('Check eligibility', `
      <div class="segmented" role="group" aria-label="Check">
        <button class="btn" data-mode="student" aria-pressed="${eligibilityMode === 'student'}">One student</button>
        <button class="btn" data-mode="company" aria-pressed="${eligibilityMode === 'company'}">One company</button>
      </div>
      ${picker}`) + '<div id="eligibility-result"></div>';
  for (const button of screen.querySelectorAll('[data-mode]')) {
    button.onclick = () => { eligibilityMode = button.dataset.mode; run(() => showEligibility(screen)); };
  }
  onPick(screen, checkEligibility);
  await checkEligibility();
}

async function checkEligibility() {
  const box = document.getElementById('eligibility-result');
  if (eligibilityMode === 'student') {
    const answer = await api('GET', '/api/eligibility/student', { rollNo: remembered.rollNo });
    box.innerHTML = studentStrip(answer.student) + twoSides(answer.rows, 'company', 'companies', companyRow);
  } else {
    const answer = await api('GET', '/api/eligibility/company', { companyId: remembered.companyId });
    box.innerHTML = companyStrip(answer.company) + twoSides(answer.rows, 'student', 'students', studentRow);
  }
}

// Splits the verdicts into Side A (eligible) and Side B (not eligible).
function twoSides(rows, one, many, toTrack) {
  const eligible = rows.filter(row => row.eligible);
  const blocked = rows.filter(row => !row.eligible);
  return `<div class="card-open">
      ${tapeSide('Side A · Eligible', plural(eligible.length, one, many), eligible.map(toTrack), 'None.')}
      ${tapeSide('Side B · Not eligible', plural(blocked.length, one, many), blocked.map(toTrack), 'None: eligible everywhere.')}
    </div>`;
}

// A company row: its criteria (a failed one struck through in red) and, on the right,
// the package if eligible or the exact gap if not.
function companyRow(row) {
  const c = row.company;
  return {
    title: esc(c.name),
    detail: joinFacts([
      mark('min CGPA ' + twoDecimals(c.minCgpa), row.cgpaFails),
      mark('max ' + plural(c.maxBacklogs, 'backlog', 'backlogs'), row.backlogsFail),
      mark(c.branches.length ? esc(c.branchesLabel) : 'all branches', row.branchFails),
    ]),
    figure: row.eligible ? twoDecimals(c.packageLpa) + ' LPA' : esc(row.remark),
    gap: !row.eligible,
  };
}

// A student row: their figures (a failing one struck through in red) and, on the right,
// the CGPA if eligible or the exact gap if not.
function studentRow(row) {
  const s = row.student;
  return {
    title: esc(s.name),
    detail: joinFacts([
      esc(s.rollNo),
      mark(esc(s.branch), row.branchFails),
      mark('CGPA ' + twoDecimals(s.cgpa), row.cgpaFails),
      mark(plural(s.activeBacklogs, 'backlog', 'backlogs'), row.backlogsFail),
    ]),
    figure: row.eligible ? twoDecimals(s.cgpa) : esc(row.remark),
    gap: !row.eligible,
  };
}
