package dev.cobbleverse.tower;

/** Screen format indices stay within the original Tower's three-value arrays. */
public record ModeChoice(int baseMode,boolean competitive) {
    public ModeChoice {
        if(baseMode<0 || baseMode>2 || competitive && baseMode!=1)throw new IllegalArgumentException("Invalid format");
    }
    public ModeChoice next() {
        if(baseMode==0)return new ModeChoice(1,false);
        if(baseMode==1 && !competitive)return new ModeChoice(1,true);
        if(baseMode==1)return new ModeChoice(2,false);
        return new ModeChoice(0,false);
    }
}
