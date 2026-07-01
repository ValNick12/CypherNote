package com.nikol.ciphernote.Network;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
//    private static final String BASE_URL = "http://192.168.1.12:3000"; //Home_laptop
//    private static final String BASE_URL = "http://192.168.1.4:3000"; //Home_desktop
    private static final String BASE_URL = "http://172.20.10.5:3000"; //Hotspot_laptop
    private static Retrofit retrofit = null;
    private static String authToken = null;

    public static void setAuthToken(String token) {
        authToken = token;
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        if (authToken == null) {
                            return chain.proceed(original);
                        }
                        Request request = original.newBuilder()
                                .header("Authorization", "Bearer " + authToken)
                                .method(original.method(), original.body())
                                .build();
                        return chain.proceed(request);
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
