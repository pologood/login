package ms.login.mapper;

import java.util.*;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.SQL;
import ms.login.entity.OpenAccount;

public interface OpenAccountMapper {
  class Sql {
    public static final String TABLE = "openAccount";
    static final String SELECT_BY_OPENID = "SELECT * FROM " + TABLE +
      " WHERE openId = #{openId}";

    static final String SELECT_BY_UID = "SELECT * FROM " + TABLE +
      " WHERE uid = #{uid}";

    static final String BIND = "INSERT INTO " + TABLE +
      "(openId, nickname, headImg, status, uid) " +
      "VALUES(#{openId}, #{nickname}, #{headImg}, #{status}, #{uid}) " +
      "ON DUPLICATE KEY UPDATE " +
      "nickname = #{nickname}, headImg = #{headImg}, status = #{status}, uid = #{uid}";

    static final String ACCEPT_BIND = "UPDATE " + TABLE +
      " SET status = #{status} WHERE openId = #{openId} AND uid = #{uid}";

    static final String DELETE = "DELETE FROM " + TABLE +
      " WHERE openId = #{openId} AND uid = #{uid}";
      
  }

  @Select(Sql.SELECT_BY_OPENID)
  OpenAccount findByOpenId(String openId);

  @Select(Sql.SELECT_BY_UID)
  List<OpenAccount> findByUid(long uid);

  @Insert(Sql.BIND)
  int bind(OpenAccount account);

  @Update(Sql.ACCEPT_BIND)
  int accept(@Param("openId") String openId, @Param("uid") long uid,
             @Param("status") OpenAccount.Status status);

  @Delete(Sql.DELETE)
  int delete(@Param("openId") String openId, @Param("uid") long uid);
}

