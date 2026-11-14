// Shared helpers used by every screen: talking to the Java server, building HTML safely,
// showing messages, and drawing the lists. Every screen file only uses what is defined here.

// The last chosen student and company, so moving between screens keeps the same selection.
const remembered = { rollNo: '', companyId: '' };

// ---------- Talking to the Java server ----------

// Calls the server and returns its JSON answer. GET sends the values in the address, POST in the body.
// The X-PlaceWise header proves a POST came from this page (WebServer refuses POSTs without it).
async function api(method, path, values = {}) {
  const params = new URLSearchParams(values);
  let response;
  try {
    if (method === 'GET') {
      response = await fetch(path + '?' + params);
    } else {
      response = await fetch(path, { method: 'POST', headers: { 'X-PlaceWise': '1' }, body: params });
    }
  } catch {
    throw new Error('Cannot reach the PlaceWise server. Is it still running in the terminal?');
  }
  const answer = await response.json();
  if (!response.ok) {
    const error = new Error(answer.error);
    error.problems = answer.problems || [];   // the validators list every broken rule
    throw error;
  }
  return answer;
}

// Runs an action; if anything goes wrong, the error is shown instead of being lost.
async function run(action) {
  try {
    await action();
  } catch (error) {
    showError(error);
  }
}

async function loadStudents() {
  return (await api('GET', '/api/students')).students;
}

async function loadCompanies() {
  return (await api('GET', '/api/companies')).companies;
}

// Imports the two sample files, exactly like choosing them in the terminal's Import menu.
async function loadSampleData() {
  const students = await api('POST', '/api/import/sample', { sample: 'students' });
  const companies = await api('POST', '/api/import/sample', { sample: 'companies' });
  showMessage('Students: ' + students.message + '. Companies: ' + companies.message + '.');
}

// ---------- Building HTML safely ----------

// Makes text safe to put inside HTML: a name such as "<b>Ann</b>" is shown as text, never run as code.
function esc(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

// "1 company", "5 companies".
function plural(count, one, many) {
  return count + ' ' + (count === 1 ? one : many);
}

// CGPA and money always with two decimals (the server sends 8.00, which JavaScript reads as 8).
function twoDecimals(number) {
  return Number(number).toFixed(2);
}

// The screen title and, on the right, the screen's own controls.
function titleRow(title, controls = '') {
  return `<header class="title-row"><h1>${title}</h1><div class="controls">${controls}</div></header>`;
}

// A drop-down of students; the remembered one is selected (or the first one).
function studentPicker(students) {
  if (!students.some(s => s.rollNo === remembered.rollNo)) {
    remembered.rollNo = students.length ? students[0].rollNo : '';
  }
  const options = students.map(s =>
    `<option value="${esc(s.rollNo)}" ${s.rollNo === remembered.rollNo ? 'selected' : ''}>`
    + `${esc(s.rollNo)} · ${esc(s.name)}</option>`).join('');
  return `<label class="field">Student<select id="pick-student">${options}</select></label>`;
}

// A drop-down of companies; the remembered one is selected (or the first one).
function companyPicker(companies) {
  if (!companies.some(c => String(c.id) === remembered.companyId)) {
    remembered.companyId = companies.length ? String(companies[0].id) : '';
  }
  const options = companies.map(c =>
    `<option value="${c.id}" ${String(c.id) === remembered.companyId ? 'selected' : ''}>${esc(c.name)}</option>`).join('');
  return `<label class="field">Company<select id="pick-company">${options}</select></label>`;
}

// Connects the pickers on a screen: remember the choice, then run the screen's update.
function onPick(screen, update) {
  const student = screen.querySelector('#pick-student');
  const company = screen.querySelector('#pick-company');
  if (student) {
    student.onchange = () => { remembered.rollNo = student.value; run(update); };
  }
  if (company) {
    company.onchange = () => { remembered.companyId = company.value; run(update); };
  }
}

// Joins short facts with a dot between them. A line may break BETWEEN facts but never inside one,
// and the dot stays at the end of a line instead of starting the next one.
function joinFacts(parts) {
  return parts.map((part, i) => `<span class="part">${part}${i < parts.length - 1 ? ' ·' : ''}</span>`).join(' ');
}

// The fixed figures of a student, always in the same order on every screen.
function studentStrip(s) {
  return `<div class="strip"><h2>${esc(s.name)}</h2>
    <p>${esc(s.rollNo)} · ${esc(s.branch)} · Semester ${s.semester} · CGPA <b>${twoDecimals(s.cgpa)}</b>
       · ${plural(s.activeBacklogs, 'backlog', 'backlogs')}</p></div>`;
}

// The criteria of a company, always in the same order on every screen.
function companyStrip(c) {
  return `<div class="strip"><h2>${esc(c.name)}</h2>
    <p>min CGPA <b>${twoDecimals(c.minCgpa)}</b> · max ${plural(c.maxBacklogs, 'backlog', 'backlogs')}
       · ${c.branches.length ? esc(c.branchesLabel) : 'all branches'} · ${twoDecimals(c.packageLpa)} LPA
       · ${plural(c.seats, 'seat', 'seats')}</p></div>`;
}

// Text that is struck through in red when a criterion is not met (and says so to screen readers).
function mark(text, failed) {
  return failed ? `<s class="fail">${text}<span class="sr"> (not met)</span></s>` : text;
}

// ---------- The list style of this design: one "side" of a tape ----------

// rows: [{ title, detail, figure, gap, pencil, no }]. title, detail and figure are HTML that the
// caller has already made safe with esc(). gap = red figure, pencil = tentative (grey) row.
// heading = null draws the list without a heading.
function tapeSide(heading, countText, rows, emptyText, numbered = true) {
  const tracks = rows.map((row, i) => `
    <li class="track${row.pencil ? ' pencil' : ''}" style="--i:${i}">
      ${numbered ? `<span class="no">${String(row.no ?? i + 1).padStart(2, '0')}</span>` : ''}
      <span class="title">${row.title}${row.detail ? `<small>${row.detail}</small>` : ''}</span>
      <span class="figure${row.gap ? ' gap' : ''}">${row.figure ?? ''}</span>
    </li>`).join('');
  const list = rows.length
    ? `<ol class="tracks ink-in${numbered ? '' : ' plain'}">${tracks}</ol>`
    : `<p class="empty">${emptyText}</p>`;
  const head = heading === null ? '' : `<h2 class="side-head"><span>${heading}</span><span>${countText}</span></h2>`;
  return `<section class="side">${head}${list}</section>`;
}

// What a screen shows when it needs students and companies but there are none yet.
function needData(screen, title) {
  screen.innerHTML = titleRow(title) + `
    <p class="empty">This screen needs students and companies first.</p>
    <button class="btn primary" id="need-data">Load the sample data</button>`;
  screen.querySelector('#need-data').onclick = () => run(async () => {
    await loadSampleData();
    route();   // draw the screen again, now with data
  });
}

// ---------- Buttons ----------

// A delete button that asks "Sure?" first: the second click within 4 seconds really deletes.
function confirmThen(button, action) {
  if (button.classList.contains('confirm')) {
    run(action);
    return;
  }
  const label = button.textContent;
  button.classList.add('confirm');
  button.textContent = 'Sure? Click again';
  setTimeout(() => {
    button.classList.remove('confirm');
    button.textContent = label;
  }, 4000);
}

// Disables a button while its action runs, so a slow action cannot be started twice.
async function busy(button, action) {
  button.disabled = true;
  try {
    await action();
  } finally {
    button.disabled = false;
  }
}

// ---------- Messages ----------

let messageTimer;

function showMessage(text) {
  showBox(text, [], false);
}

function showError(error) {
  const problems = error.problems || [];
  showBox(problems.length ? 'Not saved. Please fix:' : error.message, problems, true);
}

function showBox(text, problems, isError) {
  const box = document.getElementById('message');
  box.className = isError ? 'message error' : 'message';
  box.setAttribute('role', isError ? 'alert' : 'status');
  const list = problems.length ? '<ul>' + problems.map(p => `<li>${esc(p)}</li>`).join('') + '</ul>' : '';
  const hint = isError ? '<p class="hint">Click here to close.</p>' : '';
  box.innerHTML = `<p>${esc(text)}</p>${list}${hint}`;
  box.hidden = false;
  box.onclick = () => { box.hidden = true; };
  clearTimeout(messageTimer);
  if (!isError) {
    // a success message fades away by itself; an error stays until it is clicked, so it can be explained
    messageTimer = setTimeout(() => { box.hidden = true; }, 8000);
  }
}
