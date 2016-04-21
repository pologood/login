package ms.login.mapper;

import java.util.*;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.SQL;
import ms.login.entity.IncPerm;

public interface IncPermMapper {
  class Sql {
    final static String TABLE = "incPerm";
    final static String SELECT = "SELECT * FROM " + TABLE + " WHERE incId = #{incId}";
    final static String DELETE = "DELETE FROM " + TABLE +
      " WHERE id = #{id} AND incId = #{incId}";

    public static String insert(IncPerm perm) {
      SQL sql = new SQL().INSERT_INTO(TABLE)
        .VALUES("incId", "#{incId}")
        .VALUES("name",  "#{name}");
      
      if (perm.getDesc() != null) {
        sql.VALUES("desc", "#{desc}");
      }

      return sql.toString();
    }

    public static String update(IncPerm perm) {
      SQL sql = new SQL().UPDATE(TABLE);
      if (perm.getName() != null) {
        sql.SET("name = #{name}");
      }
      if (perm.getDesc() != null) {
        sql.SET("desc = #{desc}");
      }
      return sql.WHERE("id = #{id}").AND().WHERE("incId = #{incId}").toString();        
    }
  }

  @Select(Sql.SELECT)
  List<IncPerm> getAll(int incId);

  @InsertProvider(type = Sql.class, method = "insert")
  @Options(useGeneratedKeys=true, keyProperty = "id")
  int add(IncPerm perm);

  @UpdateProvider(type = Sql.class, method = "update")
  int update(IncPerm perm);

  @Delete(Sql.DELETE)
  int delete(@Param("id") long id, @Param("incId") int incId);
}
