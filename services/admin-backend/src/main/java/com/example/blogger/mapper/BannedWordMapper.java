package com.example.blogger.mapper;

import com.example.blogger.entity.BannedWord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BannedWordMapper {

    @Select("SELECT * FROM tu_banned_word ORDER BY created_at DESC")
    List<BannedWord> findAll();

    @Select("SELECT * FROM tu_banned_word WHERE id = #{id}")
    BannedWord findById(String id);

    @Insert("INSERT INTO tu_banned_word(id, word, replacement, category, severity, created_at) " +
            "VALUES(#{id}, #{word}, #{replacement}, #{category}, #{severity}, NOW())")
    int insert(BannedWord bannedWord);

    @Update("UPDATE tu_banned_word SET word = #{word}, replacement = #{replacement}, " +
            "category = #{category}, severity = #{severity} WHERE id = #{id}")
    int update(BannedWord bannedWord);

    @Delete("DELETE FROM tu_banned_word WHERE id = #{id}")
    int delete(String id);
}
