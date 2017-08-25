package oh.hakju.scrapper.entity;

public class Vocabulary {

    private Long vocabularyId;

    private String word;

    private String pos;

    public Long getVocabularyId() {
        return vocabularyId;
    }

    public void setVocabularyId(Long vocabularyId) {
        this.vocabularyId = vocabularyId;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    @Override
    public String toString() {
        return "Vocabulary{" +
                "vocabularyId=" + vocabularyId +
                ", word='" + word + '\'' +
                ", pos='" + pos + '\'' +
                '}';
    }
}
