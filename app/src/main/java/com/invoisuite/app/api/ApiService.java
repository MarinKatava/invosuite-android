package com.invoisuite.app.api;

import com.invoisuite.app.model.*;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;
import java.util.Map;

public interface ApiService {

    // --- Auth ---
    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/auth/google")
    Call<AuthResponse> loginWithGoogle(@Body GoogleAuthRequest request);

    @GET("api/auth/me")
    Call<User> me();

    // --- Meta ---
    @GET("api/meta/currencies")
    Call<List<CurrencyOption>> currencies();

    @GET("api/meta/units")
    Call<List<String>> units();

    // --- Companies ---
    @GET("api/companies")
    Call<List<Company>> listCompanies();

    @POST("api/companies")
    Call<Company> createCompany(@Body CompanyRequest request);

    @GET("api/companies/{id}")
    Call<Company> getCompany(@Path("id") long id);

    @PUT("api/companies/{id}")
    Call<Company> updateCompany(@Path("id") long id, @Body CompanyRequest request);

    @DELETE("api/companies/{id}")
    Call<Void> deleteCompany(@Path("id") long id);

    // --- Clients ---
    @GET("api/companies/{companyId}/clients")
    Call<List<Client>> listClients(@Path("companyId") long companyId);

    @POST("api/companies/{companyId}/clients")
    Call<Client> createClient(@Path("companyId") long companyId, @Body ClientRequest request);

    @PUT("api/companies/{companyId}/clients/{clientId}")
    Call<Client> updateClient(@Path("companyId") long companyId, @Path("clientId") long clientId, @Body ClientRequest request);

    @DELETE("api/companies/{companyId}/clients/{clientId}")
    Call<Void> deleteClient(@Path("companyId") long companyId, @Path("clientId") long clientId);

    // --- Projects ---
    @GET("api/companies/{companyId}/projects")
    Call<List<Project>> listProjects(@Path("companyId") long companyId);

    @POST("api/companies/{companyId}/projects")
    Call<Project> createProject(@Path("companyId") long companyId, @Body ProjectRequest request);

    @PUT("api/companies/{companyId}/projects/{projectId}")
    Call<Project> updateProject(@Path("companyId") long companyId, @Path("projectId") long projectId, @Body ProjectRequest request);

    @DELETE("api/companies/{companyId}/projects/{projectId}")
    Call<Void> deleteProject(@Path("companyId") long companyId, @Path("projectId") long projectId);

    // --- Invoices ---
    @GET("api/companies/{companyId}/invoices")
    Call<List<InvoiceSummary>> listInvoices(@Path("companyId") long companyId);

    @GET("api/companies/{companyId}/invoices/{invoiceId}")
    Call<Invoice> getInvoice(@Path("companyId") long companyId, @Path("invoiceId") long invoiceId);

    @GET("api/companies/{companyId}/invoices/next-number")
    Call<Map<String, String>> nextInvoiceNumber(@Path("companyId") long companyId, @Query("issueDate") String issueDate);

    @POST("api/companies/{companyId}/invoices")
    Call<Invoice> createInvoice(@Path("companyId") long companyId, @Body InvoiceRequest request);

    @PUT("api/companies/{companyId}/invoices/{invoiceId}")
    Call<Invoice> updateInvoice(@Path("companyId") long companyId, @Path("invoiceId") long invoiceId, @Body InvoiceRequest request);

    @PATCH("api/companies/{companyId}/invoices/{invoiceId}/status")
    Call<Invoice> updateInvoiceStatus(@Path("companyId") long companyId, @Path("invoiceId") long invoiceId, @Body Map<String, String> body);

    @DELETE("api/companies/{companyId}/invoices/{invoiceId}")
    Call<Void> deleteInvoice(@Path("companyId") long companyId, @Path("invoiceId") long invoiceId);

    @Streaming
    @GET("api/companies/{companyId}/invoices/{invoiceId}/pdf")
    Call<ResponseBody> downloadInvoicePdf(@Path("companyId") long companyId, @Path("invoiceId") long invoiceId);

    @Streaming
    @GET("api/companies/{companyId}/invoices/{invoiceId}/xlsx")
    Call<ResponseBody> downloadInvoiceXlsx(@Path("companyId") long companyId, @Path("invoiceId") long invoiceId);

    // --- Time tracking integrations (e.g. Odoo) ---
    @GET("api/integrations")
    Call<List<IntegrationConnection>> listIntegrations();

    @GET("api/integrations/{id}")
    Call<IntegrationConnection> getIntegration(@Path("id") long id);

    @POST("api/integrations")
    Call<IntegrationConnection> createIntegration(@Body IntegrationRequest request);

    @PUT("api/integrations/{id}")
    Call<IntegrationConnection> updateIntegration(@Path("id") long id, @Body IntegrationRequest request);

    @DELETE("api/integrations/{id}")
    Call<Void> deleteIntegration(@Path("id") long id);

    /** Tests unsaved form values; pass connectionId on edit so a blank API key reuses the stored one. */
    @POST("api/integrations/test")
    Call<ConnectionTestResult> testIntegration(@Body IntegrationRequest request, @Query("connectionId") Long connectionId);

    @POST("api/integrations/{id}/sync")
    Call<SyncResult> syncIntegration(@Path("id") long id);

    /** The "import time bookings" button: imports from all active connections, optionally for one company. */
    @POST("api/integrations/sync")
    Call<List<SyncResult>> syncAllIntegrations(@Query("companyId") Long companyId);

    @GET("api/companies/{companyId}/time-entries")
    Call<List<TimeEntry>> listTimeEntries(@Path("companyId") long companyId, @Query("from") String from, @Query("to") String to);

    @GET("api/companies/{companyId}/time-entries/summary")
    Call<List<TimeSummaryLine>> timeSummary(@Path("companyId") long companyId, @Query("clientId") long clientId,
                                            @Query("from") String from, @Query("to") String to);
}
