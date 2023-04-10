package spring.security.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL) //null 값을 가지는 필드는, JSON 응답에 포함되지 않음
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Response<T> {

    private boolean isSuccess;
    private int code;
    private String message;
    private T data;

    public static <T> Response<T> success(String message) {
        return new Response<>(true, 200, message, null);
    }

    public static <T> Response<T> success(String message, T data) {
        return new Response<>(true, 200, message, data);
    }

    public static <T> Response<T> failure(int code, String message) {
        return new Response<>(false, code, message, null);
    }
}
