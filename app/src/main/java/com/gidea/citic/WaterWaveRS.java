package com.gidea.citic;


import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.renderscript.Allocation;
import android.renderscript.Matrix4f;
import android.renderscript.Mesh;
import android.renderscript.ProgramFragment;
import android.renderscript.ProgramFragmentFixedFunction;
import android.renderscript.ProgramStore;
import android.renderscript.ProgramVertex;
import android.renderscript.ProgramVertexFixedFunction;
import android.renderscript.RenderScriptGL;
import android.renderscript.Sampler;
import android.util.DisplayMetrics;
import android.util.Log;

import java.util.TimeZone;

import static android.renderscript.ProgramStore.DepthFunc.ALWAYS;
import static android.renderscript.Sampler.Value.CLAMP;
import static android.renderscript.Sampler.Value.LINEAR;



/**
 * ━━━━━━神兽出没━━━━━━
 * 　　　┏┓　　　┏┓
 * 　　┏┛┻━━━┛┻┓
 * 　　┃　　　　　　　┃
 * 　　┃　　　━　　　┃
 * 　　┃　┳┛　┗┳　┃
 * 　　┃　　　　　　　┃
 * 　　┃　　　┻　　　┃
 * 　　┃　　　　　　　┃
 * 　　┗━┓　　　┏━┛Code is far away from bug with the animal protecting
 * 　　　　┃　　　┃    神兽保佑,代码无bug
 * 　　　　┃　　　┃
 * 　　　　┃　　　┗━━━┓
 * 　　　　┃　　　　　　　┣┓
 * 　　　　┃　　　　　　　┏┛
 * 　　　　┗┓┓┏━┳┓┏┛
 * 　　　　　┃┫┫　┃┫┫
 * 　　　　　┗┻┛　┗┻┛
 * <p>
 * 作者:jalong Han
 * 邮箱:hjl999@126.com
 * 时间:17/3/16
 * 功能:
 */

class WaterWaveRS/* extends RenderScriptScene */ {
    private static final int MESH_RESOLUTION = 48;

    private static final int RSID_STATE = 0;
    private static final int RSID_CONSTANTS = 1;
    private static final int RSID_DROP = 2;

    private static final int TEXTURES_COUNT = 2;
    private static final int RSID_TEXTURE_RIVERBED = 0;
    private static final int RSID_TEXTURE_LEAVES = 1;

    private static final String TAG = "WaterWaveRS";

    private final BitmapFactory.Options mOptionsARGB = new BitmapFactory.Options();

    @SuppressWarnings({"FieldCanBeLocal"})
    private ProgramFragment mPfBackground;
    @SuppressWarnings({"FieldCanBeLocal"})
    private ProgramFragment mPfSky;
    @SuppressWarnings({"FieldCanBeLocal"})
    private ProgramStore mPfsBackground;
    @SuppressWarnings({"FieldCanBeLocal"})
    private ProgramStore mPfsLeaf;
    @SuppressWarnings({"FieldCanBeLocal"})
    private ProgramVertex mPvSky;
    @SuppressWarnings({"FieldCanBeLocal"})
    private ProgramVertex mPvWater;
    private ProgramVertexFixedFunction.Constants mPvOrthoAlloc;
    @SuppressWarnings({"FieldCanBeLocal"})
    private Sampler mSampler;

    private int mMeshWidth;
    private Allocation mUniformAlloc;

    private int mMeshHeight;
    @SuppressWarnings({"FieldCanBeLocal"})
    private Mesh mMesh;
    private WorldState mWorldState;
    private ScriptC_WaterWave mScript;

    private ScriptField_Constants mConstants;

    private float mGlHeight;
    protected int mWidth;
    protected int mHeight;
    protected Resources mResources;
    protected RenderScriptGL mRS;

    public WaterWaveRS(int width, int height) {
//        super(width, height);
        mWidth = width;
        mHeight = height;
        mOptionsARGB.inScaled = true;
        mOptionsARGB.inPreferredConfig = Bitmap.Config.ARGB_8888;
    }

    public void init(RenderScriptGL rs, Resources res) {
        mRS = rs;
        mResources = res;
        mScript = createScript();
    }

    public void start() {
        mRS.bindRootScript(mScript);
        final WorldState worldState = mWorldState;
        final int width = worldState.width;
        final int x = width / 4 + (int) (Math.random() * (width / 2));
        final int y = worldState.height / 4 + (int) (Math.random() * (worldState.height / 2));
//        addDrop(x + (mWorldState.rotate == 0 ? (width * worldState.xOffset) : 0), y);
    }

    public void resize(int width, int height) {

        mWidth = width;
        mHeight = height;
        mWorldState.width = width;
        mWorldState.height = height;
        mWorldState.rotate = width > height ? 1 : 0;
        if (width > height) {
            mScript.set_g_glWidth(3.333f);
            mScript.set_g_glHeight(2.0f);
        } else {
            mScript.set_g_glWidth(2.0f);
            mScript.set_g_glHeight(3.333f);
        }

        mScript.set_g_rotate(mWorldState.rotate);

        mScript.invoke_initLeaves();

        Matrix4f proj = new Matrix4f();
        proj.loadProjectionNormalized(mWidth, mHeight);
        mPvOrthoAlloc.setProjection(proj);
    }

    protected ScriptC_WaterWave createScript() {

        mScript = new ScriptC_WaterWave(mRS, mResources, R.raw.waterwave);


        createMesh();
        createState();
        createProgramVertex();
        createProgramFragmentStore();
        createProgramFragment();
        loadTextures();

        mScript.setTimeZone(TimeZone.getDefault().getID());

        mScript.bind_g_Constants(mConstants);

        return mScript;
    }

    private void createMesh() {
        Mesh.TriangleMeshBuilder tmb = new Mesh.TriangleMeshBuilder(mRS, 2, 0);

        final int width = mWidth > mHeight ? mHeight : mWidth;
        final int height = mWidth > mHeight ? mWidth : mHeight;

        int wResolution = MESH_RESOLUTION;
        int hResolution = (int) (MESH_RESOLUTION * height / (float) width);

        mGlHeight = 2.0f * height / (float) width;

        wResolution += 2;
        hResolution += 2;

        for (int y = 0; y <= hResolution; y++) {
            final float yOffset = (((float) y / hResolution) * 2.f - 1.f) * height / width;
            for (int x = 0; x <= wResolution; x++) {
                tmb.addVertex(((float) x / wResolution) * 2.f - 1.f, yOffset);
            }
        }

        for (int y = 0; y < hResolution; y++) {
            final boolean shift = (y & 0x1) == 0;
            final int yOffset = y * (wResolution + 1);
            for (int x = 0; x < wResolution; x++) {
                final int index = yOffset + x;
                final int iWR1 = index + wResolution + 1;
                if (shift) {
                    tmb.addTriangle(index, index + 1, iWR1);
                    tmb.addTriangle(index + 1, iWR1 + 1, iWR1);
                } else {
                    tmb.addTriangle(index, iWR1 + 1, iWR1);
                    tmb.addTriangle(index, index + 1, iWR1 + 1);
                }
            }
        }

        mMesh = tmb.create(true);

        mMeshWidth = wResolution + 1;
        mMeshHeight = hResolution + 1;

        mScript.set_g_WaterMesh(mMesh);
    }

    static class WorldState {
        public int frameCount;
        public int width;
        public int height;
        public int meshWidth;
        public int meshHeight;
        public int rippleIndex;
        public float glWidth;
        public float glHeight;
        public float skySpeedX;
        public float skySpeedY;
        public int rotate;
        //        public int isPreview;
        public float xOffset;
    }

    private void createState() {
        mWorldState = new WorldState();
        mWorldState.width = mWidth;
        mWorldState.height = mHeight;
        mWorldState.meshWidth = mMeshWidth;
        mWorldState.meshHeight = mMeshHeight;
        mWorldState.rippleIndex = 0;
        mWorldState.glWidth = 2.0f;
        mWorldState.glHeight = mGlHeight;
        mWorldState.skySpeedX = MathUtils.random(-0.001f, 0.001f);
        mWorldState.skySpeedY = MathUtils.random(0.00008f, 0.0002f);
        mWorldState.rotate = mWidth > mHeight ? 1 : 0;
        Log.d(TAG, "________________mWorldState.rotate = " + mWorldState.rotate);
        mScript.set_g_glWidth(mWorldState.glWidth);
        mScript.set_g_glHeight(mWorldState.glHeight);
        mScript.set_g_meshWidth(mWorldState.meshWidth);
        mScript.set_g_meshHeight(mWorldState.meshHeight);
        mScript.set_g_xOffset(0);
        mScript.set_g_rotate(mWorldState.rotate);
    }

    private void loadTextures() {
        //Commented by shihaijun to disable leaves
//        mScript.set_g_TLeaves(loadTextureARGB(R.drawable.pond));
        //Commented by shihaijun
//        mScript.set_g_TRiverbed(loadRotatedTexture(R.drawable.snowenv/*keyguard_default_wallpaper*/));
        mScript.set_g_TRiverbed(/*loadLockScreenTexture(R.drawable.keyguard_default_wallpaper)*/
                loadRotatedTexture(R.drawable.pond));
    }

    private Allocation loadTexture(int id) {
        final Allocation allocation = Allocation.createFromBitmapResource(mRS, mResources, id);
//        final Allocation allocation = Allocation.createFromBitmapResource(mRS, mResources, id, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_GRAPHICS_TEXTURE);
        return allocation;
    }

    private Allocation loadRotatedTexture(int resId) {
        Bitmap bitmap = BitmapFactory.decodeResource(mResources, resId);
        final Allocation allocation = Allocation.createFromBitmap(mRS, otherRotate(bitmap, 0));
        return allocation;
    }

    private Bitmap otherRotate(Bitmap b, int degress) {

        Log.d("WaterWaveRS", "_____--- w = " + this.mWidth + "    h = " + this.mHeight +
                "   b.w = " + b.getWidth() + "  b.h = " + b.getHeight());
        if (b != null && degress != 0) {
            Matrix m = new Matrix();
            m.preScale(1.0F, -1.0F);

            try {
                Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (b != b2) {
                    b.recycle();
                    b = b2;
                }

            } catch (OutOfMemoryError outofMemory) {
                Log.e(TAG, "________Catch out of memory when create bitmap____");
            }
        }
        float ra_1 = b.getHeight() / (1.0F * b.getWidth());
        float ra_2 = mHeight / (1.0F * mWidth);

        int or_w = 0;
        int or_h = 0;

        Log.d("WaterWaveRS", " ______________------------ " + ra_1 + "  " + ra_2);

        if (ra_1 < ra_2) {
            Log.d("WaterWaveRS", "______________------------1");
            or_h = b.getHeight(); //v15
            or_w = (int) (mWidth * b.getHeight() / (float) mHeight); //v16
        } else {//>=
            Log.d("WaterWaveRS", " ______________------------ 2");
            or_h = b.getWidth();
            or_w = (int) (mHeight * b.getWidth() / (float) mWidth);
        }
        Log.d(TAG, "______________New bitmap size: " + or_w + ", " + or_h);
        Bitmap output = Bitmap.createBitmap(or_w, or_h, Bitmap.Config.ARGB_8888);
        int det_w = (b.getHeight() / 2 * 3 - mWidth) / 2;
        Canvas canvas = new Canvas(output);
//		int color = -0xbdbdde;
        Paint paint = new Paint();

        Rect rect = new Rect(0, 0, b.getWidth(), b.getHeight());
        Rect rect1 = new Rect(-det_w, 0, b.getWidth() - det_w, b.getHeight());

        RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);

        canvas.drawARGB(0, 0, 0, 0);
//		paint.setColor(Color.MAGENTA);
        canvas.drawRect(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        canvas.drawBitmap(b, rect, rect1, paint);

        return output;

    }

    private Allocation loadTextureARGB(int id) {
        Bitmap b = BitmapFactory.decodeResource(mResources, id, mOptionsARGB);
        final Allocation allocation = Allocation.createFromBitmap(mRS, b);
        return allocation;
    }

    private Allocation loadLockScreenTexture(int id) {

        Bitmap bitmap = BitmapFactory.decodeResource(mResources, id, mOptionsARGB);
        int statusBarHeight = 25;
        DisplayMetrics dm = mResources.getDisplayMetrics();
        int sWidth = dm.widthPixels;
        int sHeight = dm.heightPixels - statusBarHeight;
        Log.d(TAG, "Viewing area sWidth=" + sWidth + ",sHeight=" + sHeight);
        int bWidth = bitmap.getWidth();
        int bHeight = bitmap.getHeight();

        int dstWidth = bHeight / sHeight * sWidth;
        Log.d(TAG, "dst size width=" + dstWidth + ", height=" + bHeight);
        Bitmap clipedBitmap = clipBitmap(bitmap, dstWidth, bHeight);
        Bitmap mirroredBitmap = mirrorBitmap(clipedBitmap);

        bitmap.recycle();
        return Allocation.createFromBitmap(mRS, mirroredBitmap);
    }

    private static Bitmap clipBitmap(Bitmap bm, int width, int height) {

        if (bm == null) {
            return null;
        } else {

            Log.d(TAG, "-----Enter clipBitmap(...) bitmap size " + bm.getWidth() + "," + bm.getHeight() +
                    ", Target size:  width = " + width + ", height = " + height);

            Bitmap newbm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            newbm.setDensity(0/*DisplayMetrics.DENSITY_DEFAULT*/);
            Canvas c = new Canvas(newbm);
            c.setDensity(0/*DisplayMetrics.DENSITY_DEFAULT*/);

            Rect targetRect = new Rect();
            targetRect.top = 0;
            targetRect.left = 0;
            targetRect.right = bm.getWidth();
            targetRect.bottom = bm.getHeight();

            int deltaw = width - targetRect.right; //v2
            int deltah = height - targetRect.bottom;//v1
            Log.d(TAG, "-----deltaw = " + deltaw + ", deltah = " + deltah);
            if (deltaw > 0 || deltah <= 0) {//cond_1
                float scale = (deltaw <= deltah) ?
                        width / (float) targetRect.right : height / (float) targetRect.bottom;

                targetRect.right = (int) (targetRect.right * scale);
                targetRect.bottom = (int) (targetRect.bottom * scale);
                Log.d(TAG, "scale = " + scale + ",targetRect right = " + targetRect.right + ", bottom = " + targetRect.bottom);
                Log.d(TAG, "------------offset offsetx =" + (width - targetRect.right) / 2 + ", offsetY=" + (height - targetRect.bottom) / 2);
                targetRect.offset((width - targetRect.right) / 2, (height - targetRect.bottom) / 2);

                c.drawBitmap(bm, null, targetRect, null);

                bm.recycle();
                bm = newbm;
                Log.d(TAG, "--------------End clip bitmap, width = " + bm.getWidth() + ",height = " + bm.getHeight());
            }
            return bm;
        }
    }

    /**
     * Added by shihaijun to mirror bitmap
     *
     * @param bitmap
     * @return a new bitmap mirrored from  #{bitmap}.
     */
    private Bitmap mirrorBitmap(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.preScale(1.0F, -1.0F);

        Bitmap nBitmap = null;

        try {
            nBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//    		nBitmap.setDensity(0);
        } catch (OutOfMemoryError outOfMemory) {
            Log.e(TAG, "_____mirrorBitmap out of memory");
        }

        return nBitmap;
    }

    private Bitmap rotate(Bitmap bitmap) {

        final int w = bitmap.getWidth();
        final int h = bitmap.getHeight();
        Log.d(TAG, "_Bitmap size___w = " + w + ", h = " + h + ",____Screen size___ width = " + mWidth + ", height = " + mHeight);
        Matrix mx = new Matrix();
        mx.setScale(1f, -1f);
//    	mx.setRotate(angle);
        Bitmap step1 = Bitmap.createBitmap(bitmap, 0, 0, w, h, mx, true);

        if (bitmap != step1) {
            bitmap.recycle();
            bitmap = step1;
        }

        float f1 = bitmap.getHeight() / (float) bitmap.getWidth();
        float f2 = mHeight / (float) mWidth;
        Log.d(TAG, "scale f1 =" + f1 + ", f2 = " + f2);
        int nWidth = (f1 < f2) ? bitmap.getHeight() * mWidth / mHeight : bitmap.getWidth();
        int nHeight = (f1 < f2) ? bitmap.getHeight() : bitmap.getWidth() * mHeight / mWidth;
        Log.d(TAG, "nBitmap w=" + nWidth + ", h=" + nHeight);

        Bitmap nBitmap = Bitmap.createBitmap(nWidth, nHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(nBitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        int offset = (2 * bitmap.getWidth() / 3 - mWidth) / 2;
        Log.d(TAG, "______wallpaper offset = " + offset);
        Rect dst = new Rect(-offset, 0, bitmap.getWidth() - offset, bitmap.getHeight());
        RectF bg = new RectF(src);
        canvas.drawRect(bg, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, src, dst, paint);
        Log.d(TAG, "mBitmap : w = " + nBitmap.getWidth() + ", h = " + nBitmap.getHeight());
        return nBitmap;
    }

    private void createProgramFragment() {
        Sampler.Builder sampleBuilder = new Sampler.Builder(mRS);
        sampleBuilder.setMinification(LINEAR);
        sampleBuilder.setMagnification(LINEAR);
        sampleBuilder.setWrapS(CLAMP);
        sampleBuilder.setWrapT(CLAMP);
        mSampler = sampleBuilder.create();

        ProgramFragmentFixedFunction.Builder builder = new ProgramFragmentFixedFunction.Builder(mRS);
        builder.setTexture(ProgramFragmentFixedFunction.Builder.EnvMode.REPLACE,
                ProgramFragmentFixedFunction.Builder.Format.RGBA, 0);
        mPfBackground = builder.create();
        mPfBackground.bindSampler(mSampler, 0);

        mScript.set_g_PFBackground(mPfBackground);

        builder = new ProgramFragmentFixedFunction.Builder(mRS);
        builder.setTexture(ProgramFragmentFixedFunction.Builder.EnvMode.MODULATE,
                ProgramFragmentFixedFunction.Builder.Format.RGBA, 0);
        mPfSky = builder.create();
        mPfSky.bindSampler(mSampler, 0);

        mScript.set_g_PFSky(mPfSky);
    }


    private void createProgramFragmentStore() {
        ProgramStore.Builder builder = new ProgramStore.Builder(mRS);
        builder.setDepthFunc(ALWAYS);
        builder.setBlendFunc(ProgramStore.BlendSrcFunc.ONE, ProgramStore.BlendDstFunc.ONE);
        builder.setDitherEnabled(false);
        builder.setDepthMaskEnabled(true);
        mPfsBackground = builder.create();

        builder = new ProgramStore.Builder(mRS);
        builder.setDepthFunc(ALWAYS);
        builder.setBlendFunc(ProgramStore.BlendSrcFunc.SRC_ALPHA, ProgramStore.BlendDstFunc.ONE_MINUS_SRC_ALPHA);
        builder.setDitherEnabled(false);
        builder.setDepthMaskEnabled(true);
        mPfsLeaf = builder.create();

        mScript.set_g_PFSLeaf(mPfsLeaf);
        mScript.set_g_PFSBackground(mPfsBackground);
    }

    private void createProgramVertex() {
        mPvOrthoAlloc = new ProgramVertexFixedFunction.Constants(mRS);
        Matrix4f proj = new Matrix4f();
        proj.loadProjectionNormalized(mWidth, mHeight);
        mPvOrthoAlloc.setProjection(proj);


        ProgramVertexFixedFunction.Builder builder = new ProgramVertexFixedFunction.Builder(mRS);
        mPvSky = builder.create();
        ((ProgramVertexFixedFunction) mPvSky).bindConstants(mPvOrthoAlloc);

        mScript.set_g_PVSky(mPvSky);

        mConstants = new ScriptField_Constants(mRS, 1);
        mUniformAlloc = mConstants.getAllocation();

        ProgramVertex.Builder sb = new ProgramVertex.Builder(mRS);

        String t = "\n" +
                "varying vec4 varColor;\n" +
                "varying vec2 varTex0;\n" +

                "vec2 addDrop(vec4 d, vec2 pos, float dxMul) {\n" +
                "  vec2 ret = vec2(0.0, 0.0);\n" +
                "  vec2 delta = d.xy - pos;\n" +
                "  delta.x *= dxMul;\n" +
                "  float dist = length(delta);\n" +
                "  if (dist < d.w) { \n" +
                "    float amp = d.z * dist;\n" +
                "    amp /= d.w * d.w;\n" +
                "    amp *= sin(d.w - dist);\n" +
                "    ret = delta * amp;\n" +
                "  }\n" +
                "  return ret;\n" +
                "}\n" +

                "void main() {\n" +
                "  vec2 pos = ATTRIB_position.xy;\n" +
                "  gl_Position = vec4(pos.x, pos.y, 0.0, 1.0);\n" +
                "  float dxMul = 1.0;\n" +

//              "  varTex0 = vec2((pos.x + 1.0), (pos.y + 1.6666));\n" +
                "  varTex0 = vec2((pos.x +1.2), (-pos.y+1.15));\n" +

                "  if (UNI_Rotate < 0.9) {\n" +
//                "    varTex0.xy *= vec2(0.25, 0.33);\n" +
                "    varTex0.xy *= vec2(0.4, 0.45);\n" +
                "    varTex0.x += UNI_Offset.x * 0.5;\n" +
                "    pos.x += UNI_Offset.x * 2.0;\n" +
                "  } else {\n" +
                "    varTex0.xy *= vec2(0.5, 0.3125);\n" +
                "    dxMul = 2.5;\n" +
                "  }\n" +

                "  varColor = vec4(1.0, 1.0, 1.0, 1.0);\n" +
                "  pos.xy += vec2(1.0, 1.0);\n" +
                "  pos.xy *= vec2(25.0, 42.0);\n" +

                "  varTex0.xy += addDrop(UNI_Drop01, pos, dxMul);\n" +
                "  varTex0.xy += addDrop(UNI_Drop02, pos, dxMul);\n" +
                "  varTex0.xy += addDrop(UNI_Drop03, pos, dxMul);\n" +
                "  varTex0.xy += addDrop(UNI_Drop04, pos, dxMul);\n" +
                "  varTex0.xy += addDrop(UNI_Drop05, pos, dxMul);\n" +
                "  varTex0.xy += addDrop(UNI_Drop06, pos, dxMul);\n" +
                "  varTex0.xy += addDrop(UNI_Drop07, pos, dxMul);\n" +
                "  varTex0.xy += addDrop(UNI_Drop08, pos, dxMul);\n" +
                "  varTex0.xy += addDrop(UNI_Drop09, pos, dxMul);\n" +
                "  varTex0.xy += addDrop(UNI_Drop10, pos, dxMul);\n" +
                "}\n";

        sb.setShader(t);
        sb.addConstant(mUniformAlloc.getType());
        sb.addInput(mMesh.getVertexAllocation(0).getType().getElement());
        mPvWater = sb.create();
        mPvWater.bindConstants(mUniformAlloc, 0);

        mScript.set_g_PVWater(mPvWater);

    }

    void addDrop(float x, float y) {
        int dropX = (int) ((x / mWidth) * mMeshWidth);
        int dropY = (int) ((y / mHeight) * mMeshHeight);

        mScript.invoke_addDrop(dropX, dropY);
    }
}
