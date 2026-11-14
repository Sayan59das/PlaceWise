// Screen 9: import students or companies from CSV. Bad rows are not imported: they are written to
// rejects.csv with the reason, and shown here exactly as that file contains them.

async function showImport(screen) {
  screen.innerHTML = titleRow('Import CSV') + `
    <div class="columns">
      <section class="sheet">
        <h2>The sample files</h2>
        <p class="note">From the sample-data folder of the project.</p>
        <div class="actions-row">
          <button class="btn primary" data-sample="students">50 students</button>
          <button class="btn primary" data-sample="companies">5 companies</button>
          <button class="btn" data-sample="students-with-errors">Students file with mistakes</button>
        </div>
      </section>
      <form class="sheet" id="upload-form">
        <h2>Your own file</h2>
        <fieldset>
          <legend>The file holds</legend>
          <label class="choice"><input type="radio" name="type" value="students" checked> Students</label>
          <label class="choice"><input type="radio" name="type" value="companies"> Companies</label>
        </fieldset>
        <div class="actions-row">
          <input type="file" name="file" accept=".csv,text/csv">
          <button class="btn primary" type="submit">Import</button>
        </div>
      </form>
    </div>
    <div id="import-result"></div>`;

  for (const button of screen.querySelectorAll('[data-sample]')) {
    button.onclick = () => run(() => busy(button, async () => {
      showImportResult(await api('POST', '/api/import/sample', { sample: button.dataset.sample }));
    }));
  }
  const form = screen.querySelector('#upload-form');
  form.onsubmit = event => {
    event.preventDefault();
    run(async () => {
      const file = form.file.files[0];
      if (!file) {
        throw new Error('Choose a CSV file first.');
      }
      const csv = await file.text();   // the browser reads the file; the Java side checks every row
      showImportResult(await api('POST', '/api/import/upload', { type: form.type.value, csv }));
    });
  };
}

function showImportResult(result) {
  const rejected = result.rejectedRows.map(([line, reason, row]) => `
    <tr><td class="num">${esc(line)}</td><td class="reason">${esc(reason)}</td><td class="soft">${esc(row)}</td></tr>`).join('');
  document.getElementById('import-result').innerHTML = `
    <p class="verdict">Imported ${result.imported} of ${result.dataRows} rows; ${result.rejected} rejected.</p>
    ${result.rejected === 0 ? '' : `
      <p class="note">Written to <span class="path">${esc(String(result.rejectsFile).split(/[\\/]/).pop())}</span> in the data folder.</p>
      <div class="table-wrap"><table class="ruled">
        <caption>Rejected rows (${result.rejected})</caption>
        <thead><tr><th class="num">Line</th><th>Reason</th><th>The row in the file</th></tr></thead>
        <tbody>${rejected}</tbody>
      </table></div>`}`;
}
