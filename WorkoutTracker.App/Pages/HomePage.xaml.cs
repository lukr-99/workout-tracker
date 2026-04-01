using WorkoutTracker.App.ViewModels;

namespace WorkoutTracker.App.Pages;

public partial class HomePage : ContentPage
{
    private readonly HomeViewModel _viewModel;

    public HomePage()
    {
        InitializeComponent();
        _viewModel = ServiceHelper.GetService<HomeViewModel>();
        BindingContext = _viewModel;
    }

    protected override async void OnAppearing()
    {
        base.OnAppearing();
        await _viewModel.RefreshAsync();
    }

    private async void OnSettingsClicked(object sender, EventArgs e) =>
        await Shell.Current.GoToAsync(nameof(SettingsPage));
}
