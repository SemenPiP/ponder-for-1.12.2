package net.createmod.ponder.foundation.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.render.GlStateGuard;
import net.createmod.ponder.api.element.PonderOverlayElement;
import net.createmod.ponder.api.subject.PonderSubjectResolvers;
import net.createmod.ponder.api.subject.ResolvedPonderSubject;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderTag;
import net.createmod.ponder.foundation.element.OverlayDataElement;
import net.createmod.ponder.render.PonderWorldRenderer;
import net.createmod.ponder.render.SceneProjection;
import net.createmod.ponder.render.SceneProjection.ScreenRay;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PonderUI extends AbstractPonderScreen {
    private static final int IDENTIFY_KEY = Keyboard.KEY_Q;

    private final List<PonderScene> scenes;
    private final PonderWorldRenderer renderer = new PonderWorldRenderer();
    private int sceneIndex;
    private float yaw = -35;
    private float pitch = 25;
    private float zoom = 1;
    private float lastPartialTicks = 1;
    private float frozenPartialTicks = 1;
    private boolean dragging;
    private boolean identifyMode;
    private int lastMouseX;
    private int lastMouseY;
    private PonderProgressBar progress;
    private ItemStack identifiedStack = ItemStack.EMPTY;
    private BlockPos identifiedBlock;

    public static PonderUI of(ResourceLocation component) {
        return new PonderUI(PonderIndex.getSceneAccess().compile(component));
    }

    public static PonderUI of(ItemStack stack) {
        ResolvedPonderSubject subject = PonderSubjectResolvers.resolve(stack);
        if (!subject.isHandled())
            throw new IllegalArgumentException("Item stack could not be resolved to a Ponder component");
        return of(subject.getComponent());
    }

    public static PonderUI of(ItemStack stack, PonderTag tag) {
        return of(stack);
    }

    public PonderUI(List<PonderScene> scenes) {
        if (scenes == null || scenes.isEmpty())
            throw new IllegalArgumentException("At least one compiled Ponder scene is required");
        this.scenes = new ArrayList<PonderScene>(scenes);
        for (PonderScene scene : this.scenes) scene.begin();
    }

    @Override
    public void initGui() {
        int bottom = height - 29;
        buttonList.clear();
        addButton(new PonderButton(1, 8, bottom, 22, 20, PonderButton.Icon.INDEX)
            .withTooltip("ponder.ui.index_title")
            .withCallback(new Runnable() {
                @Override public void run() { ScreenOpener.open(new PonderIndexScreen()); }
            }));
        addButton(new PonderButton(2, width / 2 - 88, bottom, 22, 20, PonderButton.Icon.LEFT)
            .withCallback(new Runnable() {
                @Override public void run() { previousScene(); }
            }));
        addButton(new PonderButton(3, width / 2 - 62, bottom, 22, 20, PonderButton.Icon.REPLAY)
            .withCallback(new Runnable() {
                @Override public void run() { replay(); }
            }));
        addButton(new PonderButton(4, width / 2 - 36, bottom, 22, 20,
            getActiveScene().isPaused() ? PonderButton.Icon.PLAY : PonderButton.Icon.PAUSE)
            .withCallback(new Runnable() {
                @Override public void run() { togglePause(); }
            }));
        addButton(new PonderButton(5, width / 2 - 10, bottom, 22, 20, PonderButton.Icon.RIGHT)
            .withCallback(new Runnable() {
                @Override public void run() { nextScene(); }
            }));
        addButton(new PonderButton(8, width / 2 + 20, bottom, 22, 20, PonderButton.Icon.IDENTIFY)
            .active(identifyMode)
            .withTooltip("ponder.ui.identify")
            .withCallback(new Runnable() {
                @Override public void run() { toggleIdentify(); }
            }));
        addButton(new PonderButton(6, width - 30, bottom, 22, 20, PonderButton.Icon.CLOSE)
            .withCallback(new Runnable() {
                @Override public void run() { mc.displayGuiScreen(null); }
            }));
        progress = addButton(new PonderProgressBar(7, this, 42, height - 17,
            Math.max(40, width / 2 - 150), 4));
    }

    @Override
    public void updateScreen() {
        PonderScene scene = getActiveScene();
        if (!identifyMode && !scene.isPaused()) {
            scene.tick();
            renderer.tick(scene.getWorld());
        }
    }

    @Override
    protected void renderWindow(int mouseX, int mouseY, float partialTicks) {
        PonderScene scene = getActiveScene();
        if (!identifyMode && !scene.isPaused()) lastPartialTicks = partialTicks;
        float scenePartialTicks = identifyMode || scene.isPaused() ? frozenPartialTicks : partialTicks;
        clearSceneDepth();
        beforeScenePassForTesting();
        renderScene(scene, mouseX, mouseY, scenePartialTicks);
        afterScenePassForTesting();
        if (!identifyMode) {
            for (PonderOverlayElement overlay : scene.getOverlayElements())
                if (overlay.isVisible() && !(overlay instanceof OverlayDataElement))
                    overlay.render(scene, mouseX, mouseY, scenePartialTicks);
        }
        Gui.drawRect(0, 0, width, 34, 0xdd171b20);
        fontRenderer.drawString(scene.getTitle(), 12, 13, 0xffeef2f5);
        String counter = (sceneIndex + 1) + " / " + scenes.size();
        fontRenderer.drawString(counter, width - fontRenderer.getStringWidth(counter) - 12, 13, 0xffaeb8c2);
    }

    private static void clearSceneDepth() {
        try (GlStateGuard ignored = GlStateGuard.capture()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GlStateManager.depthMask(true);
            GlStateManager.clearDepth(1.0D);
            GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        }
    }

    // Package-private so the development harness can bracket the 3D pass without exposing test API.
    void beforeScenePassForTesting() {
    }

    void afterScenePassForTesting() {
    }

    private void renderScene(PonderScene scene, int mouseX, int mouseY, float partialTicks) {
        BlockPos min = scene.getWorld().getBoundsMin();
        BlockPos max = scene.getWorld().getBoundsMax();
        Vec3d center = new Vec3d((min.getX() + max.getX() + 1) / 2d,
            (min.getY() + max.getY() + 1) / 2d, (min.getZ() + max.getZ() + 1) / 2d);
        double span = Math.max(2, Math.max(max.getX() - min.getX() + 1,
            Math.max(max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1)));
        double distance = span * 2.35 * zoom / scene.getScale();
        float viewPitch = pitch + scene.getCameraPitch();
        float viewYaw = yaw + scene.getCameraYaw();
        Vec3d cameraOffset = rotateX(new Vec3d(0, -scene.getYOffset(), distance), -viewPitch);
        cameraOffset = rotateY(cameraOffset, -viewYaw);
        Vec3d sortingCamera = center.add(cameraOffset);
        int previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        boolean projectionPushed = false;
        boolean modelViewPushed = false;
        try {
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.pushMatrix();
            projectionPushed = true;
            GlStateManager.loadIdentity();
            Project.gluPerspective(45, width / (float) Math.max(1, height), .05f, 256);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.pushMatrix();
            modelViewPushed = true;
            GlStateManager.loadIdentity();
            GlStateManager.translate(0, scene.getYOffset(), -distance);
            GlStateManager.rotate(viewPitch, 1, 0, 0);
            GlStateManager.rotate(viewYaw, 0, 1, 0);
            GlStateManager.translate(-center.x, -center.y, -center.z);
            SceneProjection.captureCurrent();
            updateIdentifiedItem(scene, mouseX, mouseY, partialTicks);
            renderer.renderScene(scene, Vec3d.ZERO, sortingCamera, viewYaw - 180, viewPitch, partialTicks);
        } finally {
            if (modelViewPushed) {
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.popMatrix();
            }
            if (projectionPushed) {
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.popMatrix();
            }
            GlStateManager.matrixMode(previousMatrixMode);
        }
    }

    private void updateIdentifiedItem(PonderScene scene, int mouseX, int mouseY, float partialTicks) {
        identifiedStack = ItemStack.EMPTY;
        identifiedBlock = null;
        if (!identifyMode) return;
        if (mouseY <= 34 || mouseY >= height - 38) {
            scene.deselect();
            return;
        }
        ScreenRay ray = SceneProjection.screenRay(mouseX, mouseY);
        if (ray == null) {
            scene.deselect();
            return;
        }
        Pair<ItemStack, BlockPos> result = scene.rayTraceScene(ray.near, ray.far, partialTicks);
        identifiedStack = result.getFirst() == null ? ItemStack.EMPTY : result.getFirst();
        identifiedBlock = result.getSecond();
    }

    @Override
    protected void renderWindowForeground(int mouseX, int mouseY, float partialTicks) {
        for (GuiButton button : buttonList) {
            if (!(button instanceof PonderButton)) continue;
            PonderButton ponderButton = (PonderButton) button;
            if (ponderButton.isHoveredButton() && ponderButton.getTooltipKey() != null) {
                drawHoveringText(Arrays.asList(I18n.format(ponderButton.getTooltipKey())),
                    mouseX, mouseY, fontRenderer);
                return;
            }
        }
        if (!identifyMode || mouseY <= 34 || mouseY >= height - 38) return;
        if (!identifiedStack.isEmpty()) {
            renderToolTip(identifiedStack, mouseX, mouseY);
            return;
        }
        if (identifiedBlock != null) {
            IBlockState state = getActiveScene().getWorld().getBlockState(identifiedBlock);
            String name = state.getBlock().getLocalizedName();
            drawHoveringText(Arrays.asList(name,
                TextFormatting.DARK_GRAY + identifiedBlock.toString()), mouseX, mouseY, fontRenderer);
            return;
        }
        String help = I18n.format("ponder.ui.identify_mode", Keyboard.getKeyName(IDENTIFY_KEY));
        List<String> lines = Arrays.asList((TextFormatting.GRAY + help).split("\\n"));
        drawHoveringText(lines, mouseX, mouseY, fontRenderer);
    }

    private static Vec3d rotateX(Vec3d value, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3d(value.x, value.y * cos - value.z * sin, value.y * sin + value.z * cos);
    }

    private static Vec3d rotateY(Vec3d value, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3d(value.x * cos + value.z * sin, value.y, -value.x * sin + value.z * cos);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        if (button == 0 && !identifyMode && mouseY > 34 && mouseY < height - 38) {
            dragging = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        dragging = false;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long elapsed) {
        if (progress != null && progress.isMouseOver()) {
            progress.dragTo(mouseX);
            return;
        }
        if (!dragging) return;
        yaw += (mouseX - lastMouseX) * .5f;
        pitch = Math.max(-85, Math.min(85, pitch + (mouseY - lastMouseY) * .5f));
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) zoom = Math.max(.35f, Math.min(3, zoom * (wheel > 0 ? .9f : 1.1f)));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == IDENTIFY_KEY) {
            toggleIdentify();
            return;
        }
        if (keyCode == Keyboard.KEY_SPACE) {
            togglePause();
            return;
        }
        if (keyCode == Keyboard.KEY_R) {
            replay();
            return;
        }
        if (keyCode == Keyboard.KEY_LEFT) {
            previousScene();
            return;
        }
        if (keyCode == Keyboard.KEY_RIGHT) {
            nextScene();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void toggleIdentify() {
        identifyMode = !identifyMode;
        if (identifyMode) frozenPartialTicks = lastPartialTicks;
        else clearIdentification();
        initGui();
    }

    private void clearIdentification() {
        identifyMode = false;
        identifiedStack = ItemStack.EMPTY;
        identifiedBlock = null;
        getActiveScene().deselect();
    }

    private void togglePause() {
        if (identifyMode) {
            clearIdentification();
        } else {
            PonderScene scene = getActiveScene();
            if (!scene.isPaused()) frozenPartialTicks = lastPartialTicks;
            scene.setPaused(!scene.isPaused());
        }
        initGui();
    }

    private void replay() {
        clearIdentification();
        getActiveScene().restart();
        initGui();
    }

    private void previousScene() {
        if (sceneIndex <= 0) return;
        clearIdentification();
        sceneIndex--;
        getActiveScene().restart();
        initGui();
    }

    private void nextScene() {
        if (sceneIndex + 1 >= scenes.size()) return;
        clearIdentification();
        sceneIndex++;
        getActiveScene().restart();
        initGui();
    }

    public PonderScene getActiveScene() {
        return scenes.get(sceneIndex);
    }

    public ItemStack getSubject() {
        ResourceLocation component = getActiveScene().getComponent();
        Item item = Item.REGISTRY.getObject(component);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public void seekToTime(int tick) {
        getActiveScene().seek(tick);
    }
}
