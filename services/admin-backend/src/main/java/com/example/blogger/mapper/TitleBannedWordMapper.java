package com.example.blogger.mapper;

import com.example.blogger.entity.TitleBannedWord;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TitleBannedWordMapper {

    @Select("SELECT * FROM tu_title_banned_word WHERE is_active = 1 ORDER BY created_at DESC")
    List<TitleBannedWord> findAllActive();

    @Select("SELECT * FROM tu_title_banned_word ORDER BY created_at DESC")
    List<TitleBannedWord> findAll();

    @Select("SELECT * FROM tu_title_banned_word WHERE id = #{id}")
    TitleBannedWord findById(String id);

    @Insert("INSERT INTO tu_title_banned_word(id, word, category, is_active, created_at) " +
            "VALUES(#{id}, #{word}, #{category}, #{isActive}, NOW())")
    int insert(TitleBannedWord word);

    @Update("UPDATE tu_title_banned_word SET word=#{word}, category=#{category}, is_active=#{isActive} WHERE id=#{id}")
    int update(TitleBannedWord word);

    @Delete("DELETE FROM tu_title_banned_word WHERE id = #{id}")
    int delete(String id);
}
