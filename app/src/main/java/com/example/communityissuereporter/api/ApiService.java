package com.example.communityissuereporter.api;

import com.example.communityissuereporter.model.Issue;
import com.example.communityissuereporter.model.User;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.Map;

public interface ApiService {
    // Auth endpoints
    @POST("api/auth/signin")
    Call<AuthResponse> login(@Body LoginRequest loginRequest);

    @POST("api/auth/signup")
    Call<AuthResponse> register(@Body RegisterRequest registerRequest);

    @POST("api/auth/google")
    Call<AuthResponse> googleAuth(@Body GoogleAuthRequest googleAuthRequest);

    // User profile endpoints
    @GET("api/users/profile/{uid}")
    Call<ApiResponse<User>> getUserProfile(@Path("uid") String uid);
    
    @PUT("api/users/profile/{uid}")
    Call<ApiResponse<User>> updateUserProfile(
        @Path("uid") String uid,
        @Body UserProfileRequest request
    );
    
    @PUT("api/users/profile/{uid}/notifications")
    Call<ApiResponse<User>> updateNotificationPreferences(
        @Path("uid") String uid,
        @Body Map<String, Boolean> notificationPreferences
    );
    
    @DELETE("api/users/profile/{uid}")
    Call<ApiResponse<Void>> deleteUserAccount(@Path("uid") String uid);

    // Issue management endpoints
    @GET("api/issues")
    Call<ApiResponse<PaginatedResponse<Issue>>> getIssues(
        @Query("page") int page,
        @Query("size") int size,
        @Query("status") String status,
        @Query("location") String location,
        @Query("reporterUid") String reporterUid,
        @Query("searchTerm") String searchTerm,
        @Query("minUpvotes") Integer minUpvotes,
        @Query("startDate") String startDate,
        @Query("endDate") String endDate,
        @Query("sort") String sort
    );

    @POST("api/issues")
    Call<ApiResponse<Issue>> createIssue(@Body CreateIssueRequest request);
    
    @PUT("api/issues/{issueId}")
    Call<ApiResponse<Issue>> updateIssue(
        @Path("issueId") String issueId,
        @Body CreateIssueRequest request
    );
    
    @DELETE("api/issues/{issueId}")
    Call<ApiResponse<Void>> deleteIssue(
        @Path("issueId") String issueId,
        @Query("userId") String userId
    );

    // Image endpoints
    @Multipart
    @POST("api/images/upload")
    Call<ApiResponse<ImageUploadResponse>> uploadImage(@Part MultipartBody.Part image);
    
    @POST("api/images/upload/base64")
    Call<ApiResponse<ImageUploadResponse>> uploadBase64Image(@Body Base64ImageRequest request);
    
    @POST("api/images")
    Call<ApiResponse<ImageUploadResponse>> addImageToIssue(@Body UploadImageRequest request);
    
    @GET("api/images/{imageId}")
    Call<okhttp3.ResponseBody> getImage(@Path("imageId") String imageId);

    // Issue status and upvote endpoints
    @PATCH("api/issues/{issueId}/status")
    Call<ApiResponse<Issue>> updateIssueStatus(
        @Path("issueId") String issueId,
        @Body UpdateStatusRequest request
    );

    @POST("api/issues/{issueId}/upvote")
    Call<ApiResponse<Void>> upvoteIssue(
        @Path("issueId") String issueId,
        @Body UpvoteIssueRequest request
    );

    @GET("api/issues/{issueId}/upvote/check")
    Call<ApiResponse<Boolean>> checkUserUpvote(
        @Path("issueId") String issueId,
        @Query("userId") String userId
    );

    @GET("api/issues/{issueId}")
    Call<ApiResponse<Issue>> getIssueDetails(@Path("issueId") String issueId);
}