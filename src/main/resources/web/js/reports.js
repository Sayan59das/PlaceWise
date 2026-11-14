// Screen 8: show the four placement reports (the same tables the text and CSV exports contain)
// and write files into the data folder.

const report = { name: 'overall' };

async function showReports(screen) {
  const companies = await loadCompanies();
  const choices = [['overall', 'Overall placement summary'], ['branch', 'Branch-wise summary'],
    ['eligible', 'Eligible list of a company'], ['ranked', 'Ranked list of a company']];
  const options = choices.map(([value, label]) =>
    `<option value="${value}" ${value === report.name ? 'selected' : ''}>${label}</option>`).join('');
  const needsCompany = report.name === 'eligible' || report.name === 'ranked';
  screen.innerHTML = titleRow('Reports', `
      <label class="field">Report<select id="report-name">${options}</select></label>
      ${needsCompany && companies.length ? companyPicker(companies) : ''}`) + `
    <div id="report-table"></div>
    <h2 class="section-title">Export to files</h2>
    <div class="form-actions">
      <button class="btn primary" data-export="reports">Every report, as text and CSV</button>
      <button class="btn" data-export="students">All students as CSV</button>
      <button class="btn" data-export="companies">All companies as CSV</button>
      <button class="btn" data-export="audit">Back up the audit log</button>
    </div>
    <div id="export-result"></div>`;
  screen.querySelector('#report-name').onchange = event => {
    report.name = event.target.value;
    run(() => showReports(screen));   // the company picker appears or disappears
  };
  onPick(screen, loadReport);
  for (const button of screen.querySelectorAll('[data-export]')) {
    button.onclick = () => run(() => busy(button, async () => {
      const answer = await api('POST', '/api/export', { what: button.dataset.export });
      document.getElementById('export-result').innerHTML = tapeSide(esc(answer.message), 'in the data folder',
        answer.files.map(file => ({ title: `<span class="path">${esc(file)}</span>` })), '');
    }));
  }
  if (needsCompany && companies.length === 0) {
    document.getElementById('report-table').innerHTML = '<p class="empty">There are no companies yet.</p>';
    return;
  }
  await loadReport();
}

async function loadReport() {
  const table = await api('GET', '/api/report', { name: report.name, companyId: remembered.companyId });
  const headings = table.headers.map(h => `<th>${esc(h)}</th>`).join('');
  const rows = table.rows.map(row => '<tr>' + row.map(cell => `<td>${esc(cell)}</td>`).join('') + '</tr>').join('');
  document.getElementById('report-table').innerHTML = table.rows.length === 0
    ? `<h2>${esc(table.title)}</h2><p class="empty">This report has no rows yet.</p>`
    : `<div class="table-wrap${table.headers.length <= 2 ? ' narrow' : ''}"><table class="ruled report">
         <caption>${esc(table.title)}</caption>
         <thead><tr>${headings}</tr></thead><tbody>${rows}</tbody>
       </table></div>`;
}
