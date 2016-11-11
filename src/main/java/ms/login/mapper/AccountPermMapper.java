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
    static final String SELECT_USER_PERM = "SELECT entity, permId FROM " + TABLE +
      " WHERE uid = #{uid}";
    static final String SELECT_ENTITY_USER = "SELECT * FROM " + TABLE +
      " WHERE entity = #{entity}";

    static final String SELECT_ACCOUNT = "SELECT * FROM " + TABLE + " WHERE incId = #{incId}";
      
    static final String DELETE = "DELETE FROM " + TABLE +
      " WHERE uid = #{uid} AND incId = #{incId} AND entity = #{entity} AND permId = #{permId}";
    
    static final String DELETE_ALL = "DELETE FROM " + TABLE + " WHERE uid = #{uid}";

    static final String INSERT = "INSERT INTO " + TABLE +
      "(uid, incId, entity, permId, `grant`, `createTime`)" +
      " VALUES (#{uid}, #{incId}, #{entity}, #{permId}, #{grant}, NULL) ON DUPLICATE KEY" +
      " UPDATE incId = #{incId}, entity = #{entity}, permId = #{permId}, `grant` = #{grant}";

    static final String TRANSFER_PERM = "UPDATE " + TABLE +
      " SET uid = #{newUid}" +
      " WHERE uid = #{oldUid} AND incId = #{incId} AND entity = #{entity} AND permId = #{permId}";
  }

  @Select(Sql.SELECT)
  List<AccountPerm> getAll(long uid);

  @Select(Sql.SELECT_ACCOUNT)
  List<AccountPerm> getByIncId(int incId);

  @Select(Sql.SELECT_USER_PERM)
  List<UserPerm> get(long uid);

  @Select(Sql.SELECT_ENTITY_USER)
  List<AccountPerm> getEntityUser(String entity);

  @Insert(Sql.INSERT)
  int add(AccountPerm perm);

  @Update(Sql.TRANSFER_PERM)
  int transfer(@Param("newUid") long newUid, @Param("oldUid") long oldUid,
               @Param("incId") int incId, @Param("permId") long permId,
               @Param("entity") String entity);

  @Delete(Sql.DELETE)
  int delete(AccountPerm perm);

  @Delete(Sql.DELETE_ALL)
  int deleteAll(@Param("uid") long uid, @Param("incId") int incId);
}
