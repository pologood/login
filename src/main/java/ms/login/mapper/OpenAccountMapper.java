package ms.login.mapper;

import java.util.*;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.SQL;
import ms.login.entity.OpenAccount;

public interface OpenAccountMapper {
  class Sql {
    public static final String TABLE = "OpenAccount";
    static final String SELECT_BY_OPENID = "SELECT * FROM " + TABLE +
      " WHERE openId = #{openId}";

    static final String BIND = "INSERT INTO " + TABLE +
      "(openId, uid) VALUES(#{openId}, #{uid}) ON DUPLICATE KEY UPDATE " +
      " SET uid = #{uid} WHERE openId = #{openId}";
  }

  @Select(Sql.SELECT_BY_OPENID)
  OpenAccount findByOpenId(String openId);

  @Insert(Sql.BIND)
  int bind(@Param("openId") String openId, @Param("uid") long uid);
}

