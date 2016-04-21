package ms.login.mapper;

import java.util.*;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.SQL;
import ms.login.entity.AccountPerm;

public interface AccountPermMapper {
  class Sql {
    static final String TABLE  = "permission";
    static final String SELECT = "SELECT * FROM " + TABLE + " WHERE uid = #{uid}";
    static final String DELETE = "DELETE FROM " + TABLE +
      " WHERE uid = #{uid} AND permId = #{permId}";
    static final String DELETE_ALL = "DELETE FROM " + TABLE + " WHERE uid = #{uid}";

    public static String insert(List<AccountPerm> perms) {
      StringBuilder builder = new StringBuilder();
      builder.append("INSERT INTO ").append(TABLE).append("VALUES");
      int i = 0;
      for (AccountPerm perm : perms) {
        if (i++ != 0) builder.append(",");
        builder.append("(#{uid}, #{incId}, #{permId}, #{grant})");
      }
      return builder.toString();
    }
  }

  @Select(Sql.SELECT)
  List<AccountPerm> getAll(long uid);

  @InsertProvider(type = Sql.class, method = "insert")
  int add(List<AccountPerm> perms);

  @Delete(Sql.DELETE)
  int delete(@Param("uid") long uid, @Param("permId") long permId);

  @Delete(Sql.DELETE_ALL)
  int deleteAll(long uid);
}
