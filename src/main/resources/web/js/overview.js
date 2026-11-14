// The first screen: what is loaded, the companies and their criteria, and the overall placement numbers.

async function showOverview(screen) {
  const overview = await api('GET', '/api/overview');
  const summary = overview.summary;

  if (summary.totalStudents === 0 && summary.totalCompanies === 0) {
    screen.innerHTML = titleRow('Overview') + `
      <div class="sheet">
        <h2>Nothing is loaded yet</h2>
        <p class="note">Load the 50 sample students and 5 sample companies to start, or add your own.</p>
        <div class="actions-row">
          <button class="btn primary" id="load-samples">Load the sample data</button>
          <a class="btn" href="#import">Import a CSV file</a>
        </div>
      </div>`;
    screen.querySelector('#load-samples').onclick = event => run(() => busy(event.target, async () => {
      await loadSampleData();
      await showOverview(screen);
    }));
    return;
  }

  const companies = await loadCompanies();
  const companyRows = companies.map(c => ({
    title: esc(c.name),
    detail: joinFacts([`min CGPA ${twoDecimals(c.minCgpa)}`,
      `max ${plural(c.maxBacklogs, 'backlog', 'backlogs')}`,
      c.branches.length ? esc(c.branchesLabel) : 'all branches']),
    figure: plural(c.seats, 'seat', 'seats'),
  }));
  const branchRows = overview.branches.map(b => ({
    title: esc(b.branch),
    detail: `${plural(b.count, 'student', 'students')} · best CGPA ${twoDecimals(b.highestCgpa)}`,
    figure: 'avg ' + twoDecimals(b.averageCgpa),
  }));
  const numberRows = [
    { title: 'Students', figure: summary.totalStudents },
    { title: 'Companies', detail: plural(summary.totalSeats, 'seat', 'seats') + ' in total', figure: summary.totalCompanies },
    { title: 'Eligible for at least one company', figure: summary.eligibleForAny },
    { title: 'Eligible for no company', figure: summary.eligibleForNone, pencil: summary.eligibleForNone > 0 },
    { title: 'Companies per student, on average', figure: twoDecimals(summary.avgEligibleCompanies) },
    { title: 'Applications from the last drive', detail: summary.applications ? '' : 'run a placement drive on the stored data to fill these',
      figure: summary.applications },
    { title: 'Students placed', detail: twoDecimals(summary.placementPercent) + '% of all students', figure: summary.placedStudents },
  ];

  screen.innerHTML = titleRow('Overview', '<a class="btn primary" href="#eligibility">Check a student</a>') + `
    <div class="columns">
      <div class="stack">
        ${tapeSide('Companies', plural(companies.length, 'company', 'companies'), companyRows, 'No companies yet.')}
        ${tapeSide('Branches', plural(summary.totalStudents, 'student', 'students'), branchRows, 'No students yet.')}
      </div>
      ${tapeSide('Placement numbers', 'right now', numberRows, '', false)}
    </div>`;
}
