using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using WorkoutTracker.App.Services;
using WorkoutTracker.Core.Services;

namespace WorkoutTracker.App.ViewModels;

public sealed partial class SettingsViewModel : BaseViewModel
{
    private readonly IExportService _exportService;
    private readonly IAppThemeService _appThemeService;

    public string VersionText => $"Version {AppInfo.Current.VersionString} ({AppInfo.Current.BuildString})";
    public IReadOnlyList<string> ThemeOptions => _appThemeService.AvailableThemes.Select(theme => theme.ToString()).ToList();

    [ObservableProperty]
    private string selectedTheme = string.Empty;

    public SettingsViewModel(IExportService exportService, IAppThemeService appThemeService)
    {
        _exportService = exportService;
        _appThemeService = appThemeService;
        Title = "Settings";
        selectedTheme = _appThemeService.CurrentTheme.ToString();
    }

    [RelayCommand]
    private Task ExportAsync() =>
        RunBusyAsync(async () =>
        {
            var outputDirectory = Path.Combine(FileSystem.CacheDirectory, "exports", DateTime.UtcNow.ToString("yyyyMMdd-HHmmss"));
            var result = await _exportService.ExportAsync(outputDirectory).ConfigureAwait(false);

            var shareFiles = result.CsvPaths.Select(path => new ShareFile(path)).ToList();
            shareFiles.Add(new ShareFile(result.JsonPath));

            await MainThread.InvokeOnMainThreadAsync(() => Share.Default.RequestAsync(new ShareMultipleFilesRequest
            {
                Title = "Workout export",
                Files = shareFiles
            }));
        }, "Export created and ready to share.");

    partial void OnSelectedThemeChanged(string value)
    {
        if (!Enum.TryParse<AppThemePreference>(value, true, out var themePreference))
        {
            return;
        }

        _appThemeService.ApplyTheme(themePreference);
        StatusMessage = $"{themePreference} theme applied.";
    }
}
