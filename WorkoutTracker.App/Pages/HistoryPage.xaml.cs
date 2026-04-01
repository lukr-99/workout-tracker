using WorkoutTracker.App.ViewModels;

namespace WorkoutTracker.App.Pages;

public partial class HistoryPage : ContentPage
{
    private readonly HistoryViewModel _viewModel;

    public HistoryPage()
    {
        InitializeComponent();
        _viewModel = ServiceHelper.GetService<HistoryViewModel>();
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
