using Microsoft.Maui.Storage;
using WorkoutTracker.App.Services;

namespace WorkoutTracker.App;

public partial class App : Application
{
    public App()
    {
        InitializeComponent();

        var storedTheme = Preferences.Default.Get(AppThemeService.PreferenceKey, AppThemePreference.Dark.ToString());
        if (!Enum.TryParse<AppThemePreference>(storedTheme, true, out var themePreference))
        {
            themePreference = AppThemePreference.Dark;
        }

        UserAppTheme = AppThemeService.ToMauiTheme(themePreference);
    }

    protected override Window CreateWindow(IActivationState? activationState)
    {
        return new Window(new AppShell());
    }
}
