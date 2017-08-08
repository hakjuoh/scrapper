package oh.hakju.scrapper.entity;

/**
 * Created by hakju on 8/8/17.
 */
public class Paragraph {

    private Long paragraphId;

    private Long articleId;

    private int paragraphSeq;

    private String text;

    public Long getParagraphId() {
        return paragraphId;
    }

    public void setParagraphId(Long paragraphId) {
        this.paragraphId = paragraphId;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public int getParagraphSeq() {
        return paragraphSeq;
    }

    public void setParagraphSeq(int paragraphSeq) {
        this.paragraphSeq = paragraphSeq;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
