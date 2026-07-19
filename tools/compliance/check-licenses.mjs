import { execFileSync } from 'node:child_process';

const prohibitedLicensePatterns = [
  /^AGPL-/i,
  /^BUSL-/i,
  /^GPL-/i,
  /^SSPL-/i,
  /Commons-Clause/i,
  /^UNLICENSED$/i,
  /^UNKNOWN$/i,
];

function readLicenseInventory() {
  const output = execFileSync('pnpm', ['licenses', 'list', '--json'], {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'inherit'],
  });

  return JSON.parse(output);
}

function findProhibitedLicenses(inventory) {
  return Object.keys(inventory)
    .filter((license) => prohibitedLicensePatterns.some((pattern) => pattern.test(license)))
    .sort();
}

const inventory = readLicenseInventory();
const prohibitedLicenses = findProhibitedLicenses(inventory);

if (prohibitedLicenses.length > 0) {
  console.error('License policy check failed. Explicit legal approval is required for:');
  for (const license of prohibitedLicenses) {
    console.error(`- ${license}`);
  }
  process.exitCode = 1;
} else {
  console.log(`License policy check passed for ${Object.keys(inventory).length} license groups.`);
}
