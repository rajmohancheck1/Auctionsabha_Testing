package com.auctionsabha.tests;

import com.auctionsabha.config.TestConfig;
import com.auctionsabha.utils.TokenManager;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.testng.annotations.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Product endpoint tests.
 *
 * Covers:
 *   - GET /api/products/categories
 *   - GET /api/products/all
 *   - GET /api/products/category/{id}
 *   - GET /api/products/search?name=
 *   - POST /api/products/submit?sellerId=
 *   - GET /api/products/pending        (VERIFIER)
 *   - PUT /api/products/review/{id}    (VERIFIER)
 *   - Negative: submit without token, access pending as BUYER
 */
@Epic("Auction Sabha API")
@Feature("Products")
public class ProductTest {

    // Shared state for later tests within this class
    private static int firstCategoryId = 1;

    // ── GET /api/products/categories ─────────────────────────────────────────

    @Test(priority = 1)
    @Story("Categories")
    @Severity(SeverityLevel.CRITICAL)
    @Description("GET /api/products/categories with valid token should return a non-empty list.")
    public void getCategories_shouldReturn200WithList() {
        Response response = TestConfig.authSpec(TokenManager.adminToken)
                .get("/api/products/categories");

        System.out.println("[CATEGORIES] Status: " + response.getStatusCode());
        System.out.println("[CATEGORIES] Body  : " + response.asString());

        assertThat("Categories endpoint should return 200", response.getStatusCode(), is(200));

        // Response can be a list OR wrapped object
        String raw = response.asString().trim();
        assertThat("Categories response should not be empty", raw, not(emptyOrNullString()));

        // Try to extract first category id for subsequent tests
        try {
            List<Integer> ids = response.jsonPath().getList("id");
            if (ids != null && !ids.isEmpty() && ids.get(0) != null) {
                firstCategoryId = ids.get(0);
                System.out.println("[CATEGORIES] First category id: " + firstCategoryId);
            }
        } catch (Exception e) {
            System.out.println("[CATEGORIES] Could not parse category list: " + e.getMessage());
        }
    }

    @Test(priority = 2)
    @Story("Categories")
    @Severity(SeverityLevel.MINOR)
    @Description("GET /api/products/categories without token should be rejected.")
    public void getCategories_withoutToken_shouldBeRejected() {
        Response response = TestConfig.baseSpec()
                .get("/api/products/categories");

        System.out.println("[CATEGORIES NO TOKEN] Status: " + response.getStatusCode());

        assertThat("Categories without token should fail",
                response.getStatusCode(), anyOf(is(401), is(403)));
    }

    // ── GET /api/products/all ─────────────────────────────────────────────────

    @Test(priority = 3)
    @Story("All Products")
    @Severity(SeverityLevel.CRITICAL)
    @Description("GET /api/products/all with valid token should return 200 and a list.")
    public void getAllProducts_shouldReturn200() {
        Response response = TestConfig.authSpec(TokenManager.adminToken)
                .get("/api/products/all");

        System.out.println("[ALL PRODUCTS] Status: " + response.getStatusCode());
        System.out.println("[ALL PRODUCTS] Body  : " + response.asString().substring(0, Math.min(500, response.asString().length())));

        assertThat("Get all products should return 200", response.getStatusCode(), is(200));
        assertThat("All-products response body should not be empty",
                response.asString().trim(), not(emptyOrNullString()));
    }

    // ── GET /api/products/category/{id} ──────────────────────────────────────

    @Test(priority = 4, dependsOnMethods = "getCategories_shouldReturn200WithList")
    @Story("Products by Category")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /api/products/category/{id} should return 200.")
    public void getProductsByCategory_shouldReturn200() {
        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .get("/api/products/category/" + firstCategoryId);

        System.out.println("[PRODUCTS BY CAT] Status: " + response.getStatusCode());

        assertThat("Products by category should return 200",
                response.getStatusCode(), anyOf(is(200), is(204)));
    }

    // ── GET /api/products/search ──────────────────────────────────────────────

    @Test(priority = 5)
    @Story("Product Search")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /api/products/search?name=watch should return 200.")
    public void searchProductsByName_watch_shouldReturn200() {
        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .queryParam("name", "watch")
                .get("/api/products/search");

        System.out.println("[SEARCH] Status: " + response.getStatusCode());
        System.out.println("[SEARCH] Body  : " + response.asString());

        assertThat("Product search should return 200",
                response.getStatusCode(), anyOf(is(200), is(204)));
    }

    @Test(priority = 6)
    @Story("Product Search")
    @Severity(SeverityLevel.MINOR)
    @Description("GET /api/products/search with empty name param should return 200 or 400.")
    public void searchProductsWithEmptyName_shouldReturn200Or400() {
        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .queryParam("name", "")
                .get("/api/products/search");

        System.out.println("[SEARCH EMPTY] Status: " + response.getStatusCode());

        assertThat("Empty search should return 200 or 400",
                response.getStatusCode(), anyOf(is(200), is(400)));
    }

    // ── POST /api/products/submit ─────────────────────────────────────────────

    @Test(priority = 7)
    @Story("Submit Product")
    @Severity(SeverityLevel.BLOCKER)
    @Description("POST /api/products/submit with SELLER token should create a product and return 200/201.")
    public void submitProduct_asSeller_shouldReturn201() {
        JSONObject body = new JSONObject();
        body.put("productName",    "Vintage Gold Watch " + System.currentTimeMillis());
        body.put("description",    "A beautiful vintage timepiece from the 1960s.");
        body.put("imageUrl",       "https://example.com/images/watch.jpg");
        body.put("documentsUrl",   "https://example.com/docs/watch_cert.pdf");
        body.put("startingPrice",  5000);
        body.put("categoryId",     firstCategoryId);
        body.put("preferredDate",  "2025-12-31");

        Response response = TestConfig.authSpec(TokenManager.sellerToken)
                .queryParam("sellerId", TokenManager.sellerUserId)
                .body(body.toString())
                .post("/api/products/submit");

        System.out.println("[SUBMIT PRODUCT] Status: " + response.getStatusCode());
        System.out.println("[SUBMIT PRODUCT] Body  : " + response.asString());

        assertThat("Submit product should return 200 or 201",
                response.getStatusCode(), anyOf(is(200), is(201)));

        // Try to extract product id
        try {
            Object idObj = response.jsonPath().get("id");
            if (idObj == null) idObj = response.jsonPath().get("productId");
            if (idObj == null) idObj = response.jsonPath().get("data.id");
            if (idObj != null) {
                TokenManager.submittedProductId = Integer.parseInt(idObj.toString());
                System.out.println("[SUBMIT PRODUCT] Created productId=" + TokenManager.submittedProductId);
            }
        } catch (Exception e) {
            System.out.println("[SUBMIT PRODUCT] Could not parse product id: " + e.getMessage());
        }
    }

    @Test(priority = 8)
    @Story("Submit Product")
    @Severity(SeverityLevel.NORMAL)
    @Description("POST /api/products/submit without token should be rejected.")
    public void submitProductWithoutToken_shouldBeRejected() {
        JSONObject body = new JSONObject();
        body.put("productName",  "No Auth Watch");
        body.put("description",  "Should fail");
        body.put("startingPrice", 100);
        body.put("categoryId",   1);

        Response response = TestConfig.baseSpec()
                .queryParam("sellerId", 1)
                .body(body.toString())
                .post("/api/products/submit");

        System.out.println("[SUBMIT NO TOKEN] Status: " + response.getStatusCode());

        assertThat("Submit without token should fail",
                response.getStatusCode(), anyOf(is(401), is(403)));
    }

    @Test(priority = 9)
    @Story("Submit Product")
    @Severity(SeverityLevel.MINOR)
    @Description("POST /api/products/submit with missing required fields should return 400.")
    public void submitProductWithMissingFields_shouldReturn400() {
        JSONObject body = new JSONObject();
        // Only product name, missing required fields
        body.put("productName", "Incomplete Product");

        Response response = TestConfig.authSpec(TokenManager.sellerToken)
                .queryParam("sellerId", TokenManager.sellerUserId)
                .body(body.toString())
                .post("/api/products/submit");

        System.out.println("[SUBMIT MISSING FIELDS] Status: " + response.getStatusCode());

        assertThat("Submit with missing fields should fail",
                response.getStatusCode(), anyOf(is(400), is(422), is(500)));
    }

    // ── GET /api/products/pending (VERIFIER) ──────────────────────────────────

    @Test(priority = 10)
    @Story("Pending Products")
    @Severity(SeverityLevel.CRITICAL)
    @Description("GET /api/products/pending with VERIFIER token should return pending products list.")
    public void getPendingProducts_asVerifier_shouldReturn200() {
        if (TokenManager.verifierToken.isEmpty()) {
            System.out.println("[PENDING] VERIFIER token unavailable — skipping.");
            return;
        }

        Response response = TestConfig.authSpec(TokenManager.verifierToken)
                .get("/api/products/pending");

        System.out.println("[PENDING] Status: " + response.getStatusCode());
        System.out.println("[PENDING] Body  : " + response.asString().substring(0, Math.min(500, response.asString().length())));

        assertThat("Pending products should return 200", response.getStatusCode(), is(200));

        // Try to get a pending product id for the review test
        try {
            List<Integer> ids = response.jsonPath().getList("id");
            if (ids != null && !ids.isEmpty() && ids.get(0) != null) {
                TokenManager.submittedProductId = ids.get(0);
                System.out.println("[PENDING] Using productId=" + TokenManager.submittedProductId + " for review test.");
            }
        } catch (Exception e) {
            System.out.println("[PENDING] Could not parse ids: " + e.getMessage());
        }
    }

    @Test(priority = 11)
    @Story("Pending Products")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /api/products/pending with BUYER token should be forbidden (403).")
    public void getPendingProducts_asBuyer_shouldBeForbidden() {
        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .get("/api/products/pending");

        System.out.println("[PENDING AS BUYER] Status: " + response.getStatusCode());

        assertThat("BUYER should not access pending products",
                response.getStatusCode(), anyOf(is(401), is(403)));
    }

    // ── PUT /api/products/review/{id} (VERIFIER) ──────────────────────────────

    @Test(priority = 12, dependsOnMethods = "getPendingProducts_asVerifier_shouldReturn200")
    @Story("Review Product")
    @Severity(SeverityLevel.CRITICAL)
    @Description("PUT /api/products/review/{id}?status=APPROVED with VERIFIER token should approve product.")
    public void reviewProduct_approve_asVerifier_shouldReturn200() {
        if (TokenManager.verifierToken.isEmpty()) {
            System.out.println("[REVIEW] VERIFIER token unavailable — skipping.");
            return;
        }
        if (TokenManager.submittedProductId == 0) {
            System.out.println("[REVIEW] No product id available — skipping.");
            return;
        }

        Response response = TestConfig.authSpec(TokenManager.verifierToken)
                .queryParam("status", "APPROVED")
                .queryParam("remarks", "Looks authentic and well-documented.")
                .put("/api/products/review/" + TokenManager.submittedProductId);

        System.out.println("[REVIEW APPROVE] Status: " + response.getStatusCode());
        System.out.println("[REVIEW APPROVE] Body  : " + response.asString());

        assertThat("Review approve should return 200 or 201",
                response.getStatusCode(), anyOf(is(200), is(201)));

        TokenManager.approvedProductId = TokenManager.submittedProductId;
    }

    @Test(priority = 13)
    @Story("Review Product")
    @Severity(SeverityLevel.NORMAL)
    @Description("PUT /api/products/review/{id} with BUYER token should be forbidden.")
    public void reviewProduct_asBuyer_shouldBeForbidden() {
        if (TokenManager.submittedProductId == 0) {
            System.out.println("[REVIEW AS BUYER] No product id — skipping.");
            return;
        }

        Response response = TestConfig.authSpec(TokenManager.buyerToken)
                .queryParam("status", "APPROVED")
                .queryParam("remarks", "Unauthorized attempt")
                .put("/api/products/review/" + TokenManager.submittedProductId);

        System.out.println("[REVIEW AS BUYER] Status: " + response.getStatusCode());

        assertThat("BUYER should not review products",
                response.getStatusCode(), anyOf(is(401), is(403)));
    }
}
