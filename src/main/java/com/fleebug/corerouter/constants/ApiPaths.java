package com.fleebug.corerouter.constants;

public final class ApiPaths {

    private ApiPaths() {}

    // ── Auth ───────────────────────────────────────────────────────────
    public static final String AUTH_REGISTER    = "/api/v1/auth/register";
    public static final String AUTH_LOGIN       = "/api/v1/auth/login";
    public static final String AUTH_REQUEST_OTP = "/api/v1/auth/request-otp";
    public static final String AUTH_VERIFY_OTP = "/api/v1/auth/verify-otp";
    public static final String AUTH_GOOGLE_LOGIN = "/api/v1/auth/google/login";
    public static final String AUTH_GITHUB_LOGIN = "/api/v1/auth/github/login";
    public static final String AUTH_FORGOT_PASSWORD_REQUEST_OTP = "/api/v1/auth/forgot-password/request-otp";
    public static final String AUTH_FORGOT_PASSWORD_VERIFY_OTP = "/api/v1/auth/forgot-password/verify-otp";
    public static final String AUTH_FORGOT_PASSWORD_RESET = "/api/v1/auth/forgot-password/reset";
    public static final String AUTH_ALL         = "/api/v1/auth/**";

    // ── Models ─────────────────────────────────────────────────────────
    public static final String MODELS           = "/api/v1/models";
    public static final String MODELS_ALL       = "/api/v1/models/**";

    // ── Chat ───────────────────────────────────────────────────────────
    public static final String CHAT_COMPLETIONS = "/api/v1/chat/completions";

    // ── Wallet ─────────────────────────────────────────────────────────
    public static final String WALLET_TOPUP_INITIATE = "/api/wallet/topup/initiate";
    public static final String WALLET_TOPUP_SUCCESS = "/api/wallet/topup/success";
    public static final String WALLET_TOPUP_FAILURE = "/api/wallet/topup/failure";

    // ── Admin ──────────────────────────────────────────────────────────
    public static final String ADMIN_MODELS             = "/api/v1/admin/models/*";
    public static final String ADMIN_BILLING_CONFIG     = "/api/v1/admin/billing/config/*";
    public static final String ADMIN_BILLING_CONFIG_MODEL = "/api/v1/admin/billing/configs/model/*";
    public static final String ADMIN_BILLING_USAGE      = "/api/v1/admin/billing/usage";
    public static final String ADMIN_SERVICE_TOKENS     = "/api/v1/service-tokens/**";
    public static final String ADMIN_TECHNICAL_ALL      = "/api/v1/admin/technical/**";

    // ── Tasks ──────────────────────────────────────────────────────────
    public static final String TASKS            = "/api/v1/tasks";
    public static final String TASKS_ALL        = "/api/v1/tasks/**";
    public static final String TASKS_STATUS     = "/api/v1/tasks/status";

    // ── Internal ───────────────────────────────────────────────────────
    public static final String INTERNAL_WORKER_HEARTBEAT = "/api/v1/internal/worker/heartbeat";
    public static final String INTERNAL_ALL              = "/api/v1/internal/**";

    // ── Docs ───────────────────────────────────────────────────────────
    public static final String SCALAR_ALL       = "/scalar/**";
    public static final String API_DOCS         = "/v3/api-docs";
    public static final String API_DOCS_ALL     = "/v3/api-docs/**";
}