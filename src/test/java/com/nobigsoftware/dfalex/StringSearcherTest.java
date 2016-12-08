package com.nobigsoftware.dfalex;

import org.junit.Assert;
import org.junit.Test;

public class StringSearcherTest extends TestBase
{
    @Test
    public void test() throws Exception
    {
        DfaBuilder<JavaToken> builder = new DfaBuilder<>();
        for (JavaToken tok : JavaToken.values())
        {
            builder.addPattern(tok.m_pattern, tok);
        }
        StringSearcher<JavaToken> searcher = builder.buildStringSearcher(null);
        String instr = _readResource("SearcherTestInput.txt");
        String want = _readResource("SearcherTestOutput.txt");
        String have = searcher.findAndReplace(instr, tokenReplaceSelector);
        Assert.assertEquals(want, have);
    }

    @Test
    public void testReplaceFunc() throws Exception
    {
        SearchAndReplaceBuilder builder = new SearchAndReplaceBuilder();
        
        for (JavaToken tok : JavaToken.values())
        {
            final JavaToken t = tok;
            builder.addReplacement(tok.m_pattern, new StringReplacement() {
                @Override
                public int apply(SafeAppendable dest, CharSequence src, int s, int e) {
                    return tokenReplace(dest, t, src, s, e);
                }
            });
        }
        ObjToObj<String, String> replacer = builder.buildStringReplacer();
        String instr = _readResource("SearcherTestInput.txt");
        String want = _readResource("SearcherTestOutput.txt");
        String have = replacer.apply(instr);
        Assert.assertEquals(want, have);
    }

    @Test
    public void repositionTest() throws Exception
    {
        SearchAndReplaceBuilder builder = new SearchAndReplaceBuilder();
        builder.addReplacement(Pattern.regexI("[a-z0-9]+ +[a-z0-9]+"), 
                new StringReplacement() { public int apply(SafeAppendable dest, CharSequence src, int s, int e) {
                    for (e=s;src.charAt(e)!=' ';++e);
                    dest.append(src, s, e).append(", ");
                    for (;src.charAt(e)==' ';++e);
                    return e;
        }});
        ObjToObj<String, String> replacer = builder.buildStringReplacer();
        
        String instr = " one two  three   four five ";
        String want = " one, two, three, four, five ";
        String have = replacer.apply(instr);
        Assert.assertEquals(want, have);
    }
    
    @Test
    public void replacementDeleteIgnoreTest() throws Exception
    {
        SearchAndReplaceBuilder builder = new SearchAndReplaceBuilder();
        builder.addReplacement(Pattern.regexI("three"), StringReplacements.IGNORE);
        builder.addReplacement(Pattern.regexI("[a-z0-9]+"), StringReplacements.DELETE);
        ObjToObj<String, String> replacer = builder.buildStringReplacer();
        
        String instr = " one two  three   four five ";
        String want = "    three     ";
        String have = replacer.apply(instr);
        Assert.assertEquals(want, have);
    }
    
    @Test
    public void replacementSpaceOrNewlineTest() throws Exception
    {
        SearchAndReplaceBuilder builder = new SearchAndReplaceBuilder();
        builder.addReplacement(Pattern.regexI("[\000- ]+"), StringReplacements.SPACE_OR_NEWLINE);
        ObjToObj<String, String> replacer = builder.buildStringReplacer();
        
        String instr = "    one \n two\r\n\r\nthree  \t four\n\n\nfive ";
        String want = " one\ntwo\nthree four\nfive ";
        String have = replacer.apply(instr);
        Assert.assertEquals(want, have);
    }
    
    @Test
    public void replacementCaseTest() throws Exception
    {
        SearchAndReplaceBuilder builder = new SearchAndReplaceBuilder();
        builder.addReplacement(Pattern.regexI("u[a-zA-z]*"), StringReplacements.TOUPPER);
        builder.addReplacement(Pattern.regexI("l[a-zA-z]*"), StringReplacements.TOLOWER);
        ObjToObj<String, String> replacer = builder.buildStringReplacer();
        
        String instr = "lAbCd uAbCd";
        String want = "labcd UABCD";
        String have = replacer.apply(instr);
        Assert.assertEquals(want, have);
    }
    
    @Test
    public void replacementStringTest() throws Exception
    {
        SearchAndReplaceBuilder builder = new SearchAndReplaceBuilder();
        builder.addReplacement(Pattern.regexI("[a-zA-z]+"), StringReplacements.string("x"));
        ObjToObj<String, String> replacer = builder.buildStringReplacer();
        
        String instr = " one two  three   four five ";
        String want = " x x  x   x x ";
        String have = replacer.apply(instr);
        Assert.assertEquals(want, have);
    }
    
    @Test
    public void replacementSurroundTest() throws Exception
    {
        SearchAndReplaceBuilder builder = new SearchAndReplaceBuilder();
        builder.addReplacement(Pattern.regexI("[a-zA-z]+"), StringReplacements.surround("(", StringReplacements.TOUPPER, ")"));
        ObjToObj<String, String> replacer = builder.buildStringReplacer();
        
        String instr = " one two  three   four five ";
        String want = " (ONE) (TWO)  (THREE)   (FOUR) (FIVE) ";
        String have = replacer.apply(instr);
        Assert.assertEquals(want, have);
    }
    
    static ReplacementSelector<JavaToken> tokenReplaceSelector = new ReplacementSelector<JavaToken>() {
        @Override
        public int apply(SafeAppendable dest, JavaToken t, CharSequence src, int s, int e) {
            return tokenReplace(dest, t, src, s, e);
        }
    };
    
    static int tokenReplace(SafeAppendable dest, JavaToken mr, CharSequence src, int startPos, int endPos)
    {
        dest.append("[").append(mr.name()).append("=").append(src, startPos, endPos).append("]");
        return 0;
    }

}
