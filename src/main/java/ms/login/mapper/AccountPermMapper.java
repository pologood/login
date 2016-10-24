package ms.login.mapper;

import java.util.*;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.SQL;
import ms.login.entity.AccountPerm;
import commons.spring.RedisRememberMeService.UserPerm;

public interface AccountPermMapper {
  class Sql {
    static final String TABLE  = "permission";
    static final String SELECT = "SELECT * FROM " + TABLE + " WHERE uid = #{uid}";
    static final String SELECT_PERM = "SELECT entity, permId FROM " + TABLE + " WHERE uid = #{uid}";

    static final String SELECT_ACCOUNT = "SELECT * FROM " + TABLE + " WHERE incId = #{incId}";
      
    static final String DELETE = "DELETE FROM " + TABLE +
      " WHERE uid = #{uid} AND permId = #{permId}";
    static final String DELETE_ALL = "DELETE FROM " + TABLE + " WHERE uid = #{uid}";

    static final String INSERT = "INSERT INTO " + TABLE +
      " VALUES(#{uid}, #{incId}, #{permId}, #{grant})";
  }

  @Select(Sql.SELECT)
  List<AccountPerm> getAll(long uid);

  @Select(Sql.SELECT_ACCOUNT)
  List<AccountPerm> getByIncId(int incId);

  @Select(Sql.SELECT_PERM)
  List<UserPerm> get(long uid);

  @Insert(Sql.INSERT)
  int add(AccountPerm perms);

  @Delete(Sql.DELETE)
  int delete(@Param("uid") long uid, @Param("incId") int incId, @Param("permId") long permId);

  @Delete(Sql.DELETE_ALL)
  int deleteAll(@Param("uid") long uid, @Param("incId") int incId);
}
