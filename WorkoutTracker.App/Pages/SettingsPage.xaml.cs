using WorkoutTracker.App.ViewModels;

namespace WorkoutTracker.App.Pages;

public partial class SettingsPage : ContentPage
{
    public SettingsPage()
    {
        InitializeComponent();
        BindingContext = ServiceHelper.GetService<SettingsViewModel>();
    }
}
