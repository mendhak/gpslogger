const { test, expect } = require('@playwright/test');

test.describe('Website Navigation and Content Verification', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('file:///app/_site/index.html');
  });

  test('Verify homepage links and content', async ({ page }) => {
    // Define selectors for elements on the page
    const fdroidLink = page.locator('a[href="https://f-droid.org/packages/com.mendhak.gpslogger"]');
    const githubLink = page.locator('a[href="https://github.com/mendhak/gpslogger/releases"]').first();

    // Verify visibility of links
    await expect(githubLink).toBeVisible();
    await expect(fdroidLink).toBeVisible();

    // Navigate to FAQ and verify content
    await page.click('a:has-text("FAQ")');
    await expect(page).toHaveURL(/#frequentlyaskedquestionsandissues/);
    await expect(page.locator('text="Frequently asked questions and issues"')).toBeVisible();

    // Navigate to Screenshots and verify content
    await page.click('a:has-text("Screenshots")');
    await expect(page).toHaveURL(/#morescreenshots/);
    await expect(page.locator('text="More screenshots"')).toBeVisible();

    // Navigate to Tour and verify content
    await page.click('a:has-text("Tour")');
    await expect(page).toHaveURL(/#quicktour/);
    await expect(page.locator('text="Quick Tour"')).toBeVisible();
  });

  test('Verify Privacy Policy page', async ({ page }) => {
    await page.click('text="Read the privacy policy"');
    await expect(page).toHaveURL(/privacypolicy.html/);
    await expect(page.locator("body")).toContainText('We do not collect any personal information from you.');
  });

  test('Verify License page', async ({ page }) => {
    await page.click('text="Licensed under GPLv2"');
    await expect(page).toHaveURL(/license.html/);
    await expect(page.locator("body")).toContainText('Everyone is permitted to copy and distribute verbatim copies of this license document');

  });

  test('Verify Open Source Libraries page', async ({ page }) => {
    await page.click('text="Open source libraries used"');
    await expect(page).toHaveURL(/opensource.html/);
    await expect(page.locator("body")).toContainText('With many thanks');

  });

  test('Verify GPS Fix Details page', async ({ page }) => {
    await page.click('a[href="gps-fix-details.html"]');
    await expect(page).toHaveURL(/gps-fix-details.html/);
    await expect(page.locator("p:has-text('The almanac is not very precise')")).toBeVisible();
  });
});