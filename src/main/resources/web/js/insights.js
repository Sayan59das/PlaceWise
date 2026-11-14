// Screens 5 and 6: "why is this student not eligible?" and the what-if simulator.
// Every sentence and number here comes from the Java side (WhyNotEligibleExplainer, WhatIfSimulator).

// ---------- Screen 5: why not eligible ----------

async function showWhyNot(screen) {
  const [students, companies] = await Promise.all([loadStudents(), loadCompanies()]);
  if (students.length === 0 || companies.length === 0) {
    needData(screen, 'Why not eligible?');
    return;
  }
  screen.innerHTML = titleRow('Why not eligible?', studentPicker(students)) + '<div id="why-result"></div>';
  onPick(screen, explainStudent);
  await explainStudent();
}

async function explainStudent() {
  const answer = await api('GET', '/api/explain', { rollNo: remembered.rollNo });
  const rejections = answer.rejections;
  const eligibleCount = answer.companies - rejections.length;
  const rows = rejections.map(r => ({
    title: esc(r.company.name),
    detail: joinFacts([`min CGPA ${twoDecimals(r.company.minCgpa)}`,
      `max ${plural(r.company.maxBacklogs, 'backlog', 'backlogs')}`,
      r.company.branches.length ? esc(r.company.branchesLabel) : 'all branches']),
    figure: esc(r.summary),
    gap: true,
  }));
  let verdict;
  if (rejections.length === 0) {
    verdict = `Not rejected anywhere: eligible for all ${plural(answer.companies, 'company', 'companies')}.`;
  } else {
    verdict = `Eligible for ${eligibleCount} of ${plural(answer.companies, 'company', 'companies')}.`;
  }
  document.getElementById('why-result').innerHTML = studentStrip(answer.student)
    + `<p class="verdict">${verdict}</p>`
    + (rejections.length ? tapeSide('What each company still needs', plural(rejections.length, 'company', 'companies'), rows, '') : '')
    + `<div class="actions-row"><a class="btn" href="#what-if">Try improvements in the what-if simulator</a></div>`;
}

// ---------- Screen 6: what-if simulator ----------

const whatIf = { increase: 0, cleared: 0 };
let whatIfRequest = 0;   // counts requests, so an old answer can never overwrite a newer one

async function showWhatIf(screen) {
  const [students, companies] = await Promise.all([loadStudents(), loadCompanies()]);
  if (students.length === 0 || companies.length === 0) {
    needData(screen, 'What-if simulator');
    return;
  }
  screen.innerHTML = titleRow('What-if simulator', studentPicker(students)) + `
    <div class="sheet">
      <div class="sliders">
        <label class="field"><span>CGPA increase <output id="increase-out"></output></span>
          <input id="increase" type="range" min="0" max="2" step="0.05" value="${whatIf.increase}"></label>
        <label class="field"><span>Backlogs cleared <output id="cleared-out"></output></span>
          <input id="cleared" type="range" min="0" max="5" step="1" value="${whatIf.cleared}"></label>
      </div>
      <p class="note">Drag a slider: every company is judged again straight away. The stored record never changes,
        because the simulator judges a copy of the student.</p>
    </div>
    <div id="whatif-result"></div>`;
  const increase = screen.querySelector('#increase');
  const cleared = screen.querySelector('#cleared');
  const showValues = () => {
    screen.querySelector('#increase-out').textContent = '+' + twoDecimals(whatIf.increase);
    screen.querySelector('#cleared-out').textContent = whatIf.cleared;
  };
  // while a slider moves the rows change in place, without the write-in animation
  increase.oninput = () => { whatIf.increase = Number(increase.value); showValues(); run(() => simulate(false)); };
  cleared.oninput = () => { whatIf.cleared = Number(cleared.value); showValues(); run(() => simulate(false)); };
  showValues();
  onPick(screen, simulate);
  await simulate();
}

async function simulate(animate = true) {
  const request = ++whatIfRequest;
  const answer = await api('GET', '/api/what-if', {
    rollNo: remembered.rollNo, cgpaIncrease: twoDecimals(whatIf.increase), backlogsCleared: whatIf.cleared,
  });
  if (request !== whatIfRequest) {
    return;   // the slider moved again while this answer was on its way
  }
  const before = answer.student;
  const after = answer.scenario;
  const sideA = answer.newlyUnlocked.map(name => ({ title: esc(name), figure: '<span class="new">Unlocked</span>' }))
    .concat(answer.alreadyEligible.map(name => ({ title: esc(name), figure: 'Already' })));
  const sideB = answer.stillBlocked.map(name => ({ title: esc(name), figure: 'Still blocked', pencil: true }));
  const cohort = Object.entries(answer.cohortUnlocks).map(([company, count]) => ({
    title: esc(company), figure: plural(count, 'more student', 'more students'), pencil: count === 0,
  }));
  const box = document.getElementById('whatif-result');
  box.innerHTML = studentStrip(before)
    + `<p class="verdict">If CGPA becomes <b>${twoDecimals(after.cgpa)}</b> and backlogs become
         <b>${after.activeBacklogs}</b>: ${plural(answer.newlyUnlocked.length, 'company unlocks', 'companies unlock')}.</p>
       <div class="card-open">
         ${tapeSide('Side A · Eligible', plural(sideA.length, 'company', 'companies'), sideA, 'Still no company.')}
         ${tapeSide('Side B · Still blocked', plural(sideB.length, 'company', 'companies'), sideB, 'None: eligible everywhere.')}
       </div>`
    + tapeSide('If every student improved the same way', 'newly eligible', cohort, '', false);
  if (!animate) {
    for (const list of box.querySelectorAll('.ink-in')) {
      list.classList.remove('ink-in');
    }
  }
}
