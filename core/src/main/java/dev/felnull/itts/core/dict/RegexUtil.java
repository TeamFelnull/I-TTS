package dev.felnull.itts.core.dict;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RegexUtil {

    //ここから下、パターン群
    private final Pattern URL_REGEX = Pattern.compile("^(https?|ftp|file|s?ftp|ssh)://([\\w-]+\\.)+[\\w-]+(/[\\w\\- ./?%&=~#:,]*)?");
    private final Pattern DOMAIN_REGEX = Pattern.compile("^([a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]*\\.)+[a-zA-Z]{2,}$");
    private final Pattern IPv4_Regex = Pattern.compile("([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})");
    private final Pattern IPv6_Regex = Pattern.compile("((([0-9A-Fa-f]{1,4}:){1,6}:)|(([0-9A-Fa-f]{1,4}:){7}))([0-9A-Fa-f]{1,4})$");
    //ここから下、置き換えテキスト群
    private final String URL_REPLACE_TEXT = "ユーアールエルショウリャク";
    private final String DOMAIN_REPLACE_TEXT = "ドメインショウリャク";
    private final String IPv4_REPLACE_TEXT = "アイピーブイフォーショウリャク";
    private final String IPv6_REPLACE_TEXT = "アイピーブイロクショウリャク";

    private final  Map<Pattern, String> DictMap = new HashMap<>(); //パターンと置き換えテキストのマップ

    public RegexUtil() {
        //マップに登録
        DictMap.put(URL_REGEX,URL_REPLACE_TEXT);
        DictMap.put(DOMAIN_REGEX,DOMAIN_REPLACE_TEXT);
        DictMap.put(IPv4_Regex,IPv4_REPLACE_TEXT);
        DictMap.put(IPv6_Regex,IPv6_REPLACE_TEXT);
    }

    public String replaceText(String text) {

        //空の文字列用配列
        List<String> returnText = new ArrayList<>();

        //改行コードを空白に変換
        String replaceNewLine2SpaceText = text.replace("\n", " ");
        //空白ごとに分割
        String[] dividedSpaceTexts = replaceNewLine2SpaceText.split("\s");

        //分割されたテキストを一区切りごとに処理を実行
        for(String dividedSpaceText : dividedSpaceTexts) {
            //replacerで一致したテキストを置き換える
            String replacedText = replacer(dividedSpaceText);
            //書き出し用の配列に入れる
            returnText.add(replacedText);
        }

        //配列を一つのテキスト(空白つき)に連結してかえす
        return String.join(" ", returnText);
    }

    private String replacer(String text) {
        //MapをEntrySetに変換
        Set<Entry<Pattern, String>> EntrySet = DictMap.entrySet();

        //ループを回して、当てはまるモノが一個でもあったら終了
        for(Entry<Pattern, String> entries : EntrySet) {
            //マップから取得したパターンとテキストの比較
            Matcher matcher = entries.getKey().matcher(text);
            if(matcher.find()) { //テキスト内に一致したら
                //置き換え済みテキストをかえす
                return matcher.replaceAll(entries.getValue());
            }
        }

        return text;
    }
}

/*
  使い方
  RegexUtil rgUtil = new RegexUtil();
  String out = rgUtil.replaceText(ここにURLとかが含まれたテキスト);
*/