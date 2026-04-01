using Microsoft.Maui.Storage;

namespace WorkoutTracker.App.Services;

public enum AppThemePreference
{
    Dark,
    Light,
    System
}

public interface IAppThemeService
{
    IReadOnlyList<AppThemePreference> AvailableThemes { get; }
    AppThemePreference CurrentTheme { get; }
    void ApplyTheme(AppThemePreference themePreference);
}

public sealed class AppThemeService : IAppThemeService
{
    public const string PreferenceKey = "app-theme-preference";

    public IReadOnlyList<AppThemePreference> AvailableThemes { get; } =
    [
        AppThemePreference.Dark,
        AppThemePreference.Light,
        AppThemePreference.System
    ];

    public AppThemePreference CurrentTheme =>
        Enum.TryParse<AppThemePreference>(Preferences.Default.Get(PreferenceKey, AppThemePreference.Dark.ToString()), true, out var parsed)
            ? parsed
            : AppThemePreference.Dark;

    public void ApplyTheme(AppThemePreference themePreference)
    {
        Preferences.Default.Set(PreferenceKey, themePreference.ToString());

        if (Application.Current is not null)
        {
            Application.Current.UserAppTheme = ToMauiTheme(themePreference);
        }
    }

    public static AppTheme ToMauiTheme(AppThemePreference themePreference) =>
        themePreference switch
        {
            AppThemePreference.Light => AppTheme.Light,
            AppThemePreference.System => AppTheme.Unspecified,
            _ => AppTheme.Dark
        };
}
