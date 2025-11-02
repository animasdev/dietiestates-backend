package it.dieti.dietiestatesbackend.api.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;

/**
 * AuthSignUpConfirmPost200Response
 */
@JsonTypeName("_auth_sign_up_confirm_post_200_response")
@Generated(value = "manual", comments = "Custom DTO aligned with OpenAPI contract")
public class AuthSignUpConfirmPost200Response {

  private String status;

  public AuthSignUpConfirmPost200Response status(String status) {
    this.status = status;
    return this;
  }

  /**
   * Confirmation outcome
   * @return status
   */
  @Schema(name = "status", description = "Confirmation outcome", example = "confirmed", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuthSignUpConfirmPost200Response that = (AuthSignUpConfirmPost200Response) o;
    return Objects.equals(this.status, that.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AuthSignUpConfirmPost200Response {\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
