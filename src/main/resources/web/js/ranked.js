// Screen 4: the eligible students of a company, ranked (higher CGPA, then fewer backlogs, then name),
// with branch and minimum-CGPA filters, and page by page (page 1 with size 5 is the "top 5").

const rankedFilter = { branch: '', minCgpa: '', pageSize: '10', page: 1 };

async function showRanked(screen) {
  const [students, companies] = await Promise.all([loadStudents(), loadCompanies()]);
  if (students.length === 0 || companies.length === 0) {
    needData(screen, 'Ranked lists');
    return;
  }
  const branches = [...new Set(students.map(s => s.branch))].sort();
  const branchOptions = ['<option value="">Any branch</option>'].concat(branches.map(b =>
    `<option ${b === rankedFilter.branch ? 'selected' : ''}>${esc(b)}</option>`)).join('');
  const sizeOptions = ['5', '10', '25', '50'].map(n =>
    `<option ${n === rankedFilter.pageSize ? 'selected' : ''}>${n}</option>`).join('');
  screen.innerHTML = titleRow('Ranked lists', `
      ${companyPicker(companies)}
      <label class="field">Branch<select id="ranked-branch">${branchOptions}</select></label>
      <label class="field">Min CGPA<input id="ranked-cgpa" inputmode="decimal" size="5" value="${esc(rankedFilter.minCgpa)}" placeholder="any"></label>
      <label class="field">Per page<select id="ranked-size">${sizeOptions}</select></label>`) + `
    <div id="ranked-result"></div>`;

  // any change of company or filter starts again at page 1
  onPick(screen, () => { rankedFilter.page = 1; return loadRanked(); });
  screen.querySelector('#ranked-branch').onchange = event => { rankedFilter.branch = event.target.value; rankedFilter.page = 1; run(loadRanked); };
  screen.querySelector('#ranked-cgpa').onchange = event => { rankedFilter.minCgpa = event.target.value; rankedFilter.page = 1; run(loadRanked); };
  screen.querySelector('#ranked-size').onchange = event => { rankedFilter.pageSize = event.target.value; rankedFilter.page = 1; run(loadRanked); };
  await loadRanked();
}

async function loadRanked() {
  const answer = await api('GET', '/api/ranked', {
    companyId: remembered.companyId, branch: rankedFilter.branch, minCgpa: rankedFilter.minCgpa,
    page: rankedFilter.page, pageSize: rankedFilter.pageSize,
  });
  const pages = Math.max(1, Math.ceil(answer.total / answer.pageSize));
  const rows = answer.students.map(s => ({
    no: s.rank,
    title: esc(s.name),
    detail: `${esc(s.rollNo)} · ${esc(s.branch)} · ${plural(s.activeBacklogs, 'backlog', 'backlogs')}`,
    figure: twoDecimals(s.cgpa),
  }));
  const box = document.getElementById('ranked-result');
  box.innerHTML = companyStrip(answer.company)
    + tapeSide('Ranked, best first', plural(answer.total, 'eligible student', 'eligible students'), rows,
        'No eligible student matches these filters.')
    + `<div class="actions-row">
         <button class="btn" id="ranked-prev" ${answer.page <= 1 ? 'disabled' : ''}>Previous page</button>
         <span>Page ${answer.page} of ${pages}</span>
         <button class="btn" id="ranked-next" ${answer.page >= pages ? 'disabled' : ''}>Next page</button>
       </div>`;
  box.querySelector('#ranked-prev').onclick = () => { rankedFilter.page--; run(loadRanked); };
  box.querySelector('#ranked-next').onclick = () => { rankedFilter.page++; run(loadRanked); };
}
