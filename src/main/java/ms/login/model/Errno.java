package ms.login.model;

import org.jsondoc.core.annotation.*;

@ApiObject(name = "Errno", description = "error code")
public class Errno {
  @ApiObjectField(description = "0")
  public static final int OK                  = 0;

  @ApiObjectField(description = "400")
  public static final int BAD_REQUEST         = 400;

  @ApiObjectField(description = "401")
  public static final int UNAUTHORIZED        = 401;

  @ApiObjectField(description = "403")
  public static final int FORBIDDEN           = 403;

  @ApiObjectField(description = "404")
  public static final int NOT_FOUND           = 404;

  @ApiObjectField(description = "406")
  public static final int NOT_ACCEPT          = 406;

  @ApiObjectField(description = "500")
  public static final int INTERNAL_ERROR      = 500;

  @ApiObjectField(description = "501")
  public static final int NOT_IMPLEMENT       = 501;

  @ApiObjectField(description = "503")
  public static final int SERVICE_UNAVAILABLE = 503;
  
  @ApiObjectField(description = "1000")
  public static final int IDENTIFY_CODE_ERROR    = 1000;
  
  @ApiObjectField(description = "1001")  
  public static final int IDENTIFY_CODE_REQUIRED = 1001;
  
  @ApiObjectField(description = "1002")  
  public static final int SMS_CODE_ERROR         = 1002;

  @ApiObjectField(description = "1100")  
  public static final int USER_EXISTS             = 1100;

  @ApiObjectField(description = "1101")
  public static final int USER_NOT_FOUND          = 1101;
  
  @ApiObjectField(description = "1102")
  public static final int USER_PASSWORD_ERROR     = 1102;
  
  @ApiObjectField(description = "1103")
  public static final int EXPIRED_INVATATION_CODE = 1103;

  @ApiObjectField(description = "1104")
  public static final int INVALID_INVATATION_CODE = 1104;

  @ApiObjectField(description = "1105")
  public static final int GRANT_BOSS_ERROR        = 1105;
  
  @ApiObjectField(description = "1106")
  public static final int INC_EXISTS              = 1106;

  @ApiObjectField(description = "1200")
  public static final int OPEN_ACCESS_TOKEN_ERROR = 1200;

  public static String getMessage(int code) {
    switch (code) {
    case OK: return "OK";
    case BAD_REQUEST: return "bad request";
    case UNAUTHORIZED: return "unauthorized";
    case NOT_FOUND: return "entity not found";
    case NOT_ACCEPT: return "not acceptable";
    case INTERNAL_ERROR: return "internal server error";
    case SERVICE_UNAVAILABLE: return "service unavailable";

    case IDENTIFY_CODE_ERROR: return "identify code error";
    case IDENTIFY_CODE_REQUIRED: return "identify code required";
    case SMS_CODE_ERROR: return "sms code error";

    case USER_EXISTS: return "user exists";
    case USER_NOT_FOUND: return "user not found";
    case USER_PASSWORD_ERROR: return "password error";
    case EXPIRED_INVATATION_CODE: return "invatation code expired";
    case INVALID_INVATATION_CODE: return "invalid invatation";
    case GRANT_BOSS_ERROR: return "uid not found or is in inc.";
    case INC_EXISTS: return "uid can only be belong to one inc.";

    case OPEN_ACCESS_TOKEN_ERROR: return "get access token error";
    default: return "";
    }
  }

  public static class BadRequestException extends RuntimeException {
  }

  public static class InternalErrorException extends RuntimeException {
    public InternalErrorException(Throwable throwable) {
      super(throwable);
    }
  }
}
