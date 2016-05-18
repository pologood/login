package ms.login.model;

public class Errno {
  public static final int OK                  = 0;
  public static final int BAD_REQUEST         = 400;
  public static final int UNAUTHORIZED        = 401;
  public static final int FORBIDDEN           = 403;
  public static final int NOT_FOUND           = 404;
  public static final int NOT_ACCEPT          = 406;
  public static final int INTERNAL_ERROR      = 500;
  public static final int NOT_IMPLEMENT       = 501;
  public static final int SERVICE_UNAVAILABLE = 503;

  public static final int IDENTIFY_CODE_ERROR    = 1000;
  public static final int IDENTIFY_CODE_REQUIRED = 1001;
  public static final int SMS_CODE_ERROR         = 1002;
  
  public static final int USER_EXISTS             = 1100;
  public static final int USER_NOT_FOUND          = 1101;
  public static final int USER_PASSWORD_ERROR     = 1102;
  public static final int EXPIRED_INVATATION_CODE = 1103;
  public static final int INVALID_INVATATION_CODE = 1104;

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
