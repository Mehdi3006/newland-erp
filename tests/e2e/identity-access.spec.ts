import { expect, test } from '@playwright/test';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const identityAccessUrl = pathToFileURL(
  resolve(import.meta.dirname, '../../apps/web/identity-access/index.html'),
).toString();

test.describe('Identity & Access administration shell', () => {
  test('shows the P3.2 identity administration pages', async ({ page }) => {
    await page.goto(identityAccessUrl);

    await expect(page.getByRole('heading', { name: 'Identity administration' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Login' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Users' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Roles' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Permissions' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Assignments' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Profile' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Sessions' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Change Password' })).toBeVisible();
  });

  test('supports RTL and LTR direction switching', async ({ page }) => {
    await page.goto(identityAccessUrl);
    await expect(page.locator('html')).toHaveAttribute('dir', 'ltr');

    await page.getByRole('button', { name: 'RTL / LTR' }).click();
    await expect(page.locator('html')).toHaveAttribute('dir', 'rtl');

    await page.getByRole('button', { name: 'RTL / LTR' }).click();
    await expect(page.locator('html')).toHaveAttribute('dir', 'ltr');
  });
});
