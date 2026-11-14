// Screen 2: add, update, delete and list companies and their criteria (CompanyValidator checks them).

async function showCompanies(screen) {
  screen.innerHTML = titleRow('Companies', '<button class="btn primary" id="company-add">Add a company</button>') + `
    <form id="company-form" class="sheet form-grid" hidden></form>
    <div id="company-table"></div>`;
  screen.querySelector('#company-add').onclick = () => openCompanyForm(null);
  await loadCompanyTable();
}

async function loadCompanyTable() {
  const companies = await loadCompanies();
  const box = document.getElementById('company-table');
  if (companies.length === 0) {
    box.innerHTML = '<p class="empty">There are no companies yet. Add one, or load the sample data on the Import screen.</p>';
    return;
  }
  const rows = companies.map(c => `
    <tr>
      <td class="num">${c.id}</td><td>${esc(c.name)}</td>
      <td class="num">${twoDecimals(c.minCgpa)}</td><td class="num">${c.maxBacklogs}</td>
      <td>${c.branches.length ? esc(c.branchesLabel) : 'All'}</td>
      <td class="num">${twoDecimals(c.packageLpa)}</td><td class="num">${c.seats}</td>
      <td class="actions">
        <button class="btn small" data-edit="${c.id}">Edit</button>
        <button class="btn small danger" data-delete="${c.id}">Delete</button>
      </td>
    </tr>`).join('');
  box.innerHTML = `<p class="count">${plural(companies.length, 'company', 'companies')}</p>
    <div class="table-wrap"><table class="ruled">
      <thead><tr><th class="num">Id</th><th>Name</th><th class="num">Min CGPA</th><th class="num">Max backlogs</th>
        <th>Branches</th><th class="num">Package (LPA)</th><th class="num">Seats</th><th><span class="sr">Actions</span></th></tr></thead>
      <tbody>${rows}</tbody>
    </table></div>`;

  for (const button of box.querySelectorAll('[data-edit]')) {
    button.onclick = () => openCompanyForm(companies.find(c => String(c.id) === button.dataset.edit));
  }
  for (const button of box.querySelectorAll('[data-delete]')) {
    button.onclick = () => confirmThen(button, () => deleteCompany(button.dataset.delete));
  }
}

function openCompanyForm(company) {
  const form = document.getElementById('company-form');
  const c = company
    ? { ...company, minCgpa: twoDecimals(company.minCgpa), packageLpa: twoDecimals(company.packageLpa),
        branches: company.branches.join(', ') }
    : { id: '', name: '', minCgpa: '', maxBacklogs: '0', branches: '', packageLpa: '', seats: '' };
  form.innerHTML = `
    <h2>${company ? 'Update ' + esc(c.name) : 'Add a company'}</h2>
    <input type="hidden" name="id" value="${esc(c.id)}">
    <label class="field">Company name<input name="name" value="${esc(c.name)}"></label>
    <label class="field">Minimum CGPA<input name="minCgpa" inputmode="decimal" value="${esc(c.minCgpa)}"></label>
    <label class="field">Max active backlogs<input name="maxBacklogs" inputmode="numeric" value="${esc(c.maxBacklogs)}"></label>
    <label class="field">Allowed branches<input name="branches" value="${esc(c.branches)}" placeholder="blank = all"></label>
    <label class="field">Package (LPA)<input name="packageLpa" inputmode="decimal" value="${esc(c.packageLpa)}"></label>
    <label class="field">Seats<input name="seats" inputmode="numeric" value="${esc(c.seats)}"></label>
    <p class="note">Branches: separate with commas, for example CSE, IT. Leave blank (or write "all") for every branch.</p>
    <div class="form-actions">
      <button class="btn primary" type="submit">${company ? 'Save changes' : 'Add company'}</button>
      <button class="btn" type="button" id="company-cancel">Cancel</button>
    </div>`;
  form.hidden = false;
  form.querySelector('input[name="name"]').focus();
  form.querySelector('#company-cancel').onclick = () => { form.hidden = true; };
  form.onsubmit = event => {
    event.preventDefault();
    run(async () => {
      const path = company ? '/api/companies/update' : '/api/companies/add';
      const answer = await api('POST', path, new FormData(form));
      showMessage(answer.message);
      form.hidden = true;
      await loadCompanyTable();
    });
  };
}

async function deleteCompany(id) {
  const answer = await api('POST', '/api/companies/delete', { id });
  showMessage(answer.message);
  await loadCompanyTable();
}
