using WorkoutTracker.App.ViewModels;

namespace WorkoutTracker.App.Pages;

public partial class TemplatesPage : ContentPage
{
    private readonly TemplatesViewModel _viewModel;

    public TemplatesPage()
    {
        InitializeComponent();
        _viewModel = ServiceHelper.GetService<TemplatesViewModel>();
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
