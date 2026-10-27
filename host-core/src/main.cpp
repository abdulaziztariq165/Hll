#include "adb_bridge.h"
#include "memory_reader.h"
#include "ue4_offsets.h"
#include "entity_walker.h"
#include "aimbot.h"
#include "logger.h"
#include <iostream>
#include <thread>
#include <chrono>
#include <csignal>
#include <atomic>

static std::atomic<bool> g_running{true};

static void OnSignal(int) { g_running = false; }

int main(int argc, char** argv) {
    std::signal(SIGINT, OnSignal);
    std::signal(SIGTERM, OnSignal);

    std::string package = "com.tencent.ig";
    std::string offsetsPath = "offsets.json";
    for (int i = 1; i < argc; ++i) {
        std::string a = argv[i];
        if (a == "--package" && i + 1 < argc) package = argv[++i];
        else if (a == "--offsets" && i + 1 < argc) offsetsPath = argv[++i];
        else if (a == "--help" || a == "-h") {
            std::cout << "الاستخدام: cheat-cli [--package <اسم_الحزمة>] [--offsets <ملف>]\n"
                      << "الافتراضي: package=com.tencent.ig, offsets=offsets.json\n";
            return 0;
        }
    }

    std::cout << "═══════════════════════════════════════════\n";
    std::cout << "   أداة ألفا XK — محرك القراءة الخارجي\n";
    std::cout << "═══════════════════════════════════════════\n";

    AdbBridge adb;
    if (!adb.Connect()) {
        std::cerr << "[خطأ] فشل الاتصال بـ ADB. تأكد من تفعيل USB Debugging.\n";
        return 1;
    }
    std::cout << "[معلومة] تم الاتصال بالجهاز عبر ADB.\n";

    int pid = adb.GetPid(package);
    if (pid < 0) {
        std::cerr << "[خطأ] لم يتم العثور على العملية: " << package << "\n"
                  << "        تأكد أن اللعبة قيد التشغيل.\n";
        return 1;
    }
    std::cout << "[معلومة] PID اللعبة: " << pid << "\n";

    auto mod = adb.GetModule(pid, "libUE4.so");
    if (mod.base == 0) {
        std::cerr << "[خطأ] لم يتم العثور على libUE4.so في ذاكرة العملية.\n";
        return 1;
    }
    std::cout << "[معلومة] قاعدة libUE4.so: 0x"
              << std::hex << mod.base << std::dec
              << " (الحجم: " << mod.size << " بايت)\n";

    ue4::OffsetResolver resolver;
    if (!resolver.LoadFromFile(offsetsPath)) {
        std::cerr << "[تحذير] تعذّر تحميل ملف الإزاحات: " << offsetsPath << "\n"
                  << "        الأداة لن تعمل بشكل صحيح حتى توفّر offsets.json محدثاً.\n";
    } else {
        std::cout << "[معلومة] تم تحميل الإزاحات بنجاح.\n";
    }

    auto off = resolver.Snapshot();
    MemoryReader mem(adb, pid);
    EntityWalker walker(mem, off);

    AimbotConfig acfg;
    acfg.enabled = false;
    Aimbot aimbot(adb, pid, mod.base);
    aimbot.SetConfig(acfg);

    std::cout << "[معلومة] بدء الحلقة الرئيسية. اضغط Ctrl+C للإيقاف.\n";

    auto lastLog = std::chrono::steady_clock::now();
    while (g_running) {
        auto t0 = std::chrono::steady_clock::now();

        auto entities = walker.Snapshot();
        if (std::chrono::duration_cast<std::chrono::seconds>(t0 - lastLog).count() >= 2) {
            std::cout << "[حالة] اللاعبون المكتشفون: " << entities.size()
                      << " | ESP: " << (true ? "مفعّل" : "معطّل")
                      << " | التصويب: " << (acfg.enabled ? "مفعّل" : "معطّل")
                      << "\n";
            lastLog = t0;
        }

        auto localCtl = mem.Read<uintptr_t>(off.GWorld);
        if (localCtl && *localCtl) {
            auto view = walker.GetViewMatrix(*localCtl);
            (void)view;
        }

        auto elapsed = std::chrono::steady_clock::now() - t0;
        auto sleepMs = std::chrono::milliseconds(16) - 
                       std::chrono::duration_cast<std::chrono::milliseconds>(elapsed);
        if (sleepMs.count() > 0)
