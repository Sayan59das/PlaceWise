// Starts the page and switches between screens. The address ends in #students, #drive, ...;
// changing it (by a click on the rail or a key press) draws that screen.

const SCREENS = {
  'overview': showOverview,
  'students': showStudents,
  'companies': showCompanies,
  'eligibility': showEligibility,
  'ranked': showRanked,
  'why-not': showWhyNot,
  'what-if': showWhatIf,
  'drive': showDrive,
  'reports': showReports,
  'import': showImport,
};

// The same numbers as the terminal menu: press 1 for Students, 7 for the placement drive, 0 for the overview.
const KEYS = ['overview', 'students', 'companies', 'eligibility', 'ranked', 'why-not', 'what-if', 'drive',
  'reports', 'import'];

function route() {
  let name = location.hash.slice(1);
  if (!SCREENS[name]) {
    name = 'overview';
  }
  for (const link of document.querySelectorAll('.nav a')) {
    if (link.hash === '#' + name) {
      link.setAttribute('aria-current', 'page');
      link.scrollIntoView({ inline: 'center', block: 'nearest' });   // matters on a phone, where the rail scrolls sideways
    } else {
      link.removeAttribute('aria-current');
    }
  }
  document.getElementById('message').hidden = true;   // an old message belongs to the old screen
  const screen = document.getElementById('screen');
  screen.innerHTML = '';
  run(() => SCREENS[name](screen));
}

window.addEventListener('hashchange', () => {
  route();
  document.getElementById('screen').focus();   // keyboard and screen-reader users land on the new screen
});

document.addEventListener('keydown', event => {
  const typing = event.target.closest('input, select, textarea');
  if (typing || event.ctrlKey || event.altKey || event.metaKey) {
    return;
  }
  const index = '0123456789'.indexOf(event.key);
  if (index >= 0) {
    location.hash = KEYS[index];
  }
});

// Show where the data lives (in memory or MySQL) under the menu, then draw the first screen.
run(async () => {
  const overview = await api('GET', '/api/overview');
  document.getElementById('storage').textContent = 'Storage: ' + overview.storage;
});
route();
