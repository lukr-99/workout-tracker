using CommunityToolkit.Mvvm.Input;
using WorkoutTracker.Core.Services;

namespace WorkoutTracker.App.ViewModels;

public sealed partial class SettingsViewModel : BaseViewModel
{
    private readonly IExportService _exportService;

    public string VersionText => $"Version {AppInfo.Current.VersionString} ({AppInfo.Current.BuildString})";

    public SettingsViewModel(IExportService exportService)
    {
        _exportService = exportService;
        Title = "Settings";
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
}
