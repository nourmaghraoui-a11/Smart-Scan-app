package com.example.myapplication.UI;

import com.example.myapplication.Data.AiRepository;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface GrokApi {

    @POST("v1/chat/completions")
    Call<AiRepository.GrokResponse> generate(
            @Header("Authorization") String token,
            @Body AiRepository.GrokRequest body
    );
}
