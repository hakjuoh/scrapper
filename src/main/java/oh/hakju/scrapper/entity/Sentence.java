package oh.hakju.scrapper.entity;

public class Sentence {

    private Long sentenceId;

    private Long articleId;

    private int paragraphSeq;

    private int sentenceSeq;

    private String text;

    public Long getSentenceId() {
        return sentenceId;
    }

    public void setSentenceId(Long sentenceId) {
        this.sentenceId = sentenceId;
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

    public int getSentenceSeq() {
        return sentenceSeq;
    }

    public void setSentenceSeq(int sentenceSeq) {
        this.sentenceSeq = sentenceSeq;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
