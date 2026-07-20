import { expect, test } from '@playwright/test';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const enterpriseStructureUrl = pathToFileURL(
  resolve(import.meta.dirname, '../../apps/web/enterprise-structure/index.html'),
).toString();

test.describe('Enterprise Structure administration shell', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${enterpriseStructureUrl}#list`);
  });

  test('shows list filters, hierarchy navigation, and empty/loading/error states', async ({
    page,
  }) => {
    await expect(page).toHaveTitle(/Enterprise Structure/u);
    await expect(
      page.getByRole('heading', { name: 'Enterprise Structure Administration' }),
    ).toBeVisible();
    await expect(
      page.getByRole('navigation', { name: 'Enterprise Structure routes' }),
    ).toBeVisible();
    await expect(page.getByRole('region', { name: 'Hierarchy navigation' })).toContainText(
      'Warehouse location',
    );
    await expect(page.getByLabel('Code or name search')).toBeVisible();
    await expect(page.getByLabel('Status')).toBeVisible();
    await expect(page.getByText('No enterprise structure records exist yet.')).toBeVisible();
    await expect(page.locator('#loading-state')).toBeHidden();
    await expect(page.locator('#error-state')).toBeHidden();

    await page.getByLabel('Code or name search').fill('NL');
    await page.getByLabel('Status').selectOption('ACTIVE');

    await expect(page.getByLabel('Code or name search')).toHaveValue('NL');
    await expect(page.getByLabel('Status')).toHaveValue('ACTIVE');
  });

  test('supports create, detail, and edit route smoke navigation', async ({ page }) => {
    const routeAssertions = [
      { hash: '#create', heading: 'Create enterprise structure node' },
      { hash: '#detail', heading: 'Enterprise structure detail' },
      { hash: '#edit', heading: 'Edit enterprise structure node' },
    ];

    for (const routeAssertion of routeAssertions) {
      await page.goto(`${enterpriseStructureUrl}${routeAssertion.hash}`);
      await expect(page.getByRole('heading', { name: routeAssertion.heading })).toBeVisible();
    }
  });

  test('shows guarded deactivation and optimistic-lock conflict states', async ({ page }) => {
    await page.goto(`${enterpriseStructureUrl}#edit`);

    await page.getByRole('button', { name: 'Open deactivation confirmation' }).click();
    await expect(
      page.getByText(
        'Deactivation requires confirmation and active-child checks from the backend.',
      ),
    ).toBeVisible();

    await page.getByRole('button', { name: 'Simulate optimistic-lock conflict' }).click();
    await expect(
      page.getByText('Optimistic-lock conflict: reload the record before saving.'),
    ).toBeVisible();
  });

  test('switches from English LTR to Persian RTL', async ({ page }) => {
    await expect(page.locator('html')).toHaveAttribute('lang', 'en');
    await expect(page.locator('html')).toHaveAttribute('dir', 'ltr');

    await page.getByRole('button', { name: 'فارسی / RTL' }).click();

    await expect(page.locator('html')).toHaveAttribute('lang', 'fa');
    await expect(page.locator('html')).toHaveAttribute('dir', 'rtl');
    await expect(page.getByRole('heading', { name: 'مدیریت ساختار سازمانی' })).toBeVisible();
  });
});
