package dev.cobbleverse.tower;

/** Screen format indices stay within the original Tower's three-value arrays. */
public record ModeChoice(int baseMode,boolean competitive,boolean open) {
    public ModeChoice(int baseMode,boolean competitive){this(baseMode,competitive,false);}
    public ModeChoice {
        if(baseMode<0 || baseMode>2 || competitive && baseMode!=1 || open && !competitive)throw new IllegalArgumentException("Invalid format");
    }
    public ModeChoice next() { return next(false); }
    public ModeChoice next(boolean openSupported) {
        if(baseMode==0)return new ModeChoice(1,false);
        if(baseMode==1 && !competitive)return new ModeChoice(1,true);
        if(baseMode==1 && !open && openSupported)return new ModeChoice(1,true,true);
        if(baseMode==1)return new ModeChoice(2,false);
        return new ModeChoice(0,false);
    }
}
