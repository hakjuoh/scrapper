package oh.hakju.scrapper.entity;

public class ContentRelation {

    private Long contentRelationId;

    private Long sourceContentId;

    private Long targetContentId;

    private String linkText;

    public Long getContentRelationId() {
        return contentRelationId;
    }

    public void setContentRelationId(Long contentRelationId) {
        this.contentRelationId = contentRelationId;
    }

    public Long getSourceContentId() {
        return sourceContentId;
    }

    public void setSourceContentId(Long sourceContentId) {
        this.sourceContentId = sourceContentId;
    }

    public Long getTargetContentId() {
        return targetContentId;
    }

    public void setTargetContentId(Long targetContentId) {
        this.targetContentId = targetContentId;
    }

    public String getLinkText() {
        return linkText;
    }

    public void setLinkText(String linkText) {
        this.linkText = linkText;
    }
}
