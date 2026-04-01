using Microsoft.Extensions.Logging;
using WorkoutTracker.App.Pages;
using WorkoutTracker.App.Services;
using WorkoutTracker.App.ViewModels;
using WorkoutTracker.Core.Data;
using WorkoutTracker.Core.Services;

namespace WorkoutTracker.App;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        var builder = MauiApp.CreateBuilder();
        builder
            .UseMauiApp<App>()
            .ConfigureFonts(fonts =>
            {
                fonts.AddFont("OpenSans-Regular.ttf", "OpenSansRegular");
                fonts.AddFont("OpenSans-Semibold.ttf", "OpenSansSemibold");
            });

        var databasePath = Path.Combine(FileSystem.AppDataDirectory, "workout-tracker.db3");
        builder.Services.AddSingleton(new HttpClient());
        builder.Services.AddSingleton(_ => new WorkoutTrackerRepository(databasePath));
        builder.Services.AddSingleton<IWorkoutDataService>(sp => sp.GetRequiredService<WorkoutTrackerRepository>());
        builder.Services.AddSingleton<IWorkoutHistoryService>(sp => sp.GetRequiredService<WorkoutTrackerRepository>());
        builder.Services.AddSingleton<IAnalyticsService>(sp => sp.GetRequiredService<WorkoutTrackerRepository>());
        builder.Services.AddSingleton<IExerciseCatalogSyncService, WgerSyncService>();
        builder.Services.AddSingleton<IExportService, ExportService>();
        builder.Services.AddSingleton<IAppThemeService, AppThemeService>();
        builder.Services.AddSingleton<IAppDialogService, AppDialogService>();

        builder.Services.AddTransient<HomeViewModel>();
        builder.Services.AddTransient<TemplatesViewModel>();
        builder.Services.AddTransient<ExerciseCatalogViewModel>();
        builder.Services.AddTransient<HistoryViewModel>();
        builder.Services.AddTransient<SettingsViewModel>();
        builder.Services.AddTransient<WorkoutEditorViewModel>();
        builder.Services.AddTransient<WorkoutDetailViewModel>();

        builder.Services.AddTransient<HomePage>();
        builder.Services.AddTransient<TemplatesPage>();
        builder.Services.AddTransient<ExerciseCatalogPage>();
        builder.Services.AddTransient<HistoryPage>();
        builder.Services.AddTransient<SettingsPage>();
        builder.Services.AddTransient<WorkoutEditorPage>();
        builder.Services.AddTransient<WorkoutDetailPage>();

#if DEBUG
        builder.Logging.AddDebug();
#endif

        var app = builder.Build();
        ServiceHelper.Initialize(app.Services);
        app.Services.GetRequiredService<IWorkoutDataService>().InitializeAsync().GetAwaiter().GetResult();
        return app;
    }
}
