package io.github.squid233.test;

import overrungl.glfw.GLFW;
import overrungl.glfw.GLFWFramebufferSizeFun;
import overrungl.glfw.GLFWVidMode;
import overrungl.opengl.GL;
import overrungl.util.MemoryStack;
import overrungl.util.MemoryUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static overrungl.glfw.GLFW.*;
import static overrungl.opengl.GL10.GL_FLOAT;
import static overrungl.opengl.GL10.GL_TRIANGLES;
import static overrungl.opengl.GL15.GL_ARRAY_BUFFER;
import static overrungl.opengl.GL15.GL_STATIC_DRAW;
import static overrungl.opengl.GL20.GL_FRAGMENT_SHADER;
import static overrungl.opengl.GL20.GL_VERTEX_SHADER;

public class OglNativeImg {
    GL gl;

    void run() {
        if (!glfwInit()) {
            throw new IllegalStateException("failed to initialize GLFW");
        }
        GLFWVidMode vidMode = GLFWVidMode.ofNative(glfwGetVideoMode(glfwGetPrimaryMonitor()));
        if (vidMode != null) {
            glfwWindowHint(GLFW_POSITION_X, (vidMode.width() - 800) / 2);
            glfwWindowHint(GLFW_POSITION_Y, (vidMode.height() - 600) / 2);
        }
        MemorySegment window;
        try (MemoryStack stack = MemoryStack.pushLocal()) {
            window = glfwCreateWindow(800, 600, stack.allocateFrom("Hello world"), MemorySegment.NULL, MemorySegment.NULL);
        }
        if (MemoryUtil.isNullPointer(window)) {
            throw new IllegalStateException("failed to create window");
        }
        glfwMakeContextCurrent(window);
        gl = new GL(GLFW::glfwGetProcAddress);
        gl.ClearColor(0.4f, 0.6f, 0.9f, 1.0f);
        glfwSetFramebufferSizeCallback(window, GLFWFramebufferSizeFun.alloc(Arena.global(), this::onResize));
        int program = gl.CreateProgram();
        int vsh = gl.CreateShader(GL_VERTEX_SHADER);
        int fsh = gl.CreateShader(GL_FRAGMENT_SHADER);
        try (Arena arena = Arena.ofConfined()) {
            gl.ShaderSource(vsh, 1, arena.allocateFrom(ValueLayout.ADDRESS, arena.allocateFrom("""
                    #version 330
                    layout(location=0) in vec2 Position;
                    layout(location=1) in vec3 Color;
                    out vec4 vertexColor;
                    void main() {
                        gl_Position = vec4(Position, 0.0, 1.0);
                        vertexColor = vec4(Color, 1.0);
                    }""")), MemorySegment.NULL);
            gl.ShaderSource(fsh, 1, arena.allocateFrom(ValueLayout.ADDRESS, arena.allocateFrom("""
                    #version 330
                    in vec4 vertexColor;
                    out vec4 fragColor;
                    void main() {
                        fragColor = vertexColor;
                    }""")), MemorySegment.NULL);
        }
        gl.CompileShader(vsh);
        gl.CompileShader(fsh);
        gl.AttachShader(program, vsh);
        gl.AttachShader(program, fsh);
        gl.LinkProgram(program);
        gl.DetachShader(program, vsh);
        gl.DetachShader(program, fsh);
        gl.DeleteShader(vsh);
        gl.DeleteShader(fsh);
        int vao, vbo;
        try (MemoryStack stack = MemoryStack.pushLocal()) {
            MemorySegment p = stack.allocate(ValueLayout.JAVA_INT);
            gl.GenVertexArrays(1, p);
            vao = p.get(ValueLayout.JAVA_INT, 0);
            gl.GenBuffers(1, p);
            vbo = p.get(ValueLayout.JAVA_INT, 0);
        }
        gl.BindVertexArray(vao);
        gl.BindBuffer(GL_ARRAY_BUFFER, vbo);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocateFrom(ValueLayout.JAVA_FLOAT,
                    0.0f, 0.5f, 1.0f, 0.0f, 0.0f,
                    -0.5f, -0.5f, 0.0f, 1.0f, 0.0f,
                    0.5f, -0.5f, 0.0f, 0.0f, 1.0f);
            gl.BufferData(GL_ARRAY_BUFFER, segment.byteSize(), segment, GL_STATIC_DRAW);
        }
        gl.EnableVertexAttribArray(0);
        gl.EnableVertexAttribArray(1);
        gl.VertexAttribPointer(0, 2, GL_FLOAT, false, 5 * 4, MemorySegment.NULL);
        gl.VertexAttribPointer(1, 3, GL_FLOAT, false, 5 * 4, MemorySegment.ofAddress(2 * 4));
        gl.BindBuffer(GL_ARRAY_BUFFER, 0);
        gl.UseProgram(program);
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            gl.DrawArrays(GL_TRIANGLES, 0, 3);
            glfwSwapBuffers(window);
        }
        gl.DeleteProgram(program);
        try (MemoryStack stack = MemoryStack.pushLocal()) {
            gl.DeleteVertexArrays(1, stack.ints(vao));
            gl.DeleteBuffers(1, stack.ints(vbo));
        }
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    void onResize(MemorySegment window, int width, int height) {
        gl.Viewport(0, 0, width, height);
    }

    static void main() {
        new OglNativeImg().run();
    }
}
